/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.csvw.metadata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find metadata info for a given CSV file, using various methods
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataFinder extends CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataFinder.class);

	private static final String WELL_KNOWN = "/.well-known/csvm";
	private static final String METADATA_EXT = "-metadata.json";
	private static final String METADATA_CSV = "csv-metadata.json";
	private static final String CSV = ".csv";

	private final URL csvLocation;

	private final static Pattern LINK = Pattern.compile(
			"<([^>].*)>(.*rel=\"describedby\")");

	// "<([^>].*)>(?=.*type=\"application/(csvm\\+json|ld\\+json|json)\")(?=.*rel=\"describedBy\")");

	/**
	 * Check if the HTTP Link Header contains an URL matching
	 *
	 * @return list of URLs
	 */
	private List<URL> linkHeader() {
		List<URL> urls = new ArrayList<>();

		URI uri;
		try {
			uri = csvLocation.toURI();
		} catch (URISyntaxException ex) {
			LOGGER.error("Invalid link header URL {}", csvLocation, ex);
			return urls;
		}

		HttpRequest head = HttpRequest.newBuilder()
				.uri(uri)
				.method("HEAD", BodyPublishers.noBody())
				.build();

		try {
			HttpResponse<byte[]> response = CSVWMetadataUtil.HTTP_CLIENT.send(head, BodyHandlers.ofByteArray());

			List<String> headers = response.headers().allValues("Link");
			for (String header : headers) {
				Matcher m = LINK.matcher(header);
				if (m.matches()) {
					String file = m.group(1);
					// last should be used first
					urls.add(0, uri.resolve(file).toURL());
					LOGGER.info("Link header {}", urls.get(0));
				}
			}
		} catch (IOException ex) {
			LOGGER.error("Could not open URL {}", csvLocation, ex);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return urls;
	}

	/**
	 * Check if there is a file in a ".well-known" location on the server.
	 *
	 * This file may contain a list of URL templates to construct possible URLs for a metadata file
	 *
	 * @return list of URLs
	 */
	private List<URL> wellKnownURL() {
		List<URL> urls = new ArrayList<>();

		URI uri;
		try {
			uri = csvLocation.toURI().resolve(WELL_KNOWN);
		} catch (URISyntaxException ex) {
			LOGGER.error("Invalid well-known metadata URL", ex);
			return urls;
		}

		byte[] buffer = CSVWMetadataUtil.tryURI(uri);
		if (buffer == null) {
			return urls;
		}

		try (ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
				InputStreamReader is = new InputStreamReader(bais);
				BufferedReader r = new BufferedReader(is)) {
			URI metaURI;
			String line = r.readLine();

			while (line != null) {
				String s = line.replaceFirst("\\{\\+?url\\}", csvLocation.toString());
				if (s.isBlank()) {
					continue;
				}
				switch (line.charAt(0)) {
				case '?':
					metaURI = URI.create(line + s);
					break;
				case '/':
					metaURI = uri.resolve(s);
					break;
				default:
					metaURI = URI.create(s);
				}
				if (metaURI != null) {
					urls.add(metaURI.toURL());
				}
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.error("Could not parse {}", uri);
		}
		return urls;
	}

	/**
	 * Check if there is a "-metadata.json" file relative to the location of the CSV file (while keeping the .csv
	 * extension)
	 *
	 * @return URL
	 */
	private URL specificURLExtension() {
		try {
			return new URL(csvLocation.toString() + METADATA_EXT);
		} catch (MalformedURLException ex) {
			LOGGER.error("Incorrect specific metadata URL (with extension)", ex);
			return null;
		}
	}

	/**
	 * Check if there is a "-metadata.json" file relative to the location of the CSV file. (after removal of the .csv
	 * extension)
	 *
	 * @return URL
	 */
	private URL specificURL() {
		String s = csvLocation.toString();
		if (s.endsWith(CSV)) {
			s = s.substring(0, s.length() - CSV.length());
		}
		try {
			return new URL(s + METADATA_EXT);
		} catch (MalformedURLException ex) {
			LOGGER.error("Incorrect specific metadata URL", ex);
			return null;
		}
	}

	/**
	 * Check if there is a generic "csv-metadata.json" on the server
	 *
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	private URL genericURL() {
		try {
			return csvLocation.toURI().resolve(METADATA_CSV).toURL();
		} catch (URISyntaxException | MalformedURLException ex) {
			LOGGER.error("Incorrect generic metadata URL", ex);
			return null;
		}
	}

	/**
	 * Try different ways of finding the JSON metadata file
	 *
	 * @see https://w3c.github.io/csvw/syntax/#locating-metadata
	 * @return
	 */
	@Override
	public InputStream getMetadata() {
		if (csvLocation == null) {
			LOGGER.error("Location of CSV file unknown, metadata location cannot be derived");
			return null;
		}

		List<URL> urls = linkHeader();
		urls.addAll(wellKnownURL());
		urls.add(specificURLExtension());
		urls.add(specificURL());
		urls.add(genericURL());

		byte[] bytes = null;

		for (URL url : urls) {
			try {
				bytes = CSVWMetadataUtil.tryURI(url.toURI());
				if (bytes != null) {
					break;
				}
			} catch (URISyntaxException ex) {
				LOGGER.error("Invalid URL {}", url, ex);
			}
		}

		return (bytes != null) ? new ByteArrayInputStream(bytes) : null;
	}

	/**
	 * Constructor
	 *
	 * @param csvLocation
	 */
	public CSVWMetadataFinder(URL csvLocation) {
		this.csvLocation = csvLocation;
	}
}
