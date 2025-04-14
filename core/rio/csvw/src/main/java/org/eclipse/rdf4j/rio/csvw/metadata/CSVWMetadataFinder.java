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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find metadata info for a given CSV file, using various methods
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataFinder implements CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataFinder.class);

	private static final String WELL_KNOWN = "/.well-known/csvm";
	private static final String METADATA_EXT = "-metadata.json";
	private static final String METADATA_CSV = "csv-metadata.json";
	private static final String CSV = ".csv";

	private final URL csvLocation;

	/**
	 * Check if there is a file in a ".well-known" location on the server.
	 *
	 * This file may contain a list of URL templates to construct possible URLs for a metadata file
	 *
	 * @return list of URLs
	 */
	private List<URL> wellKnownURL() {
		List<URL> urls = new ArrayList<>();

		URL url;
		try {
			url = csvLocation.toURI().resolve(WELL_KNOWN).toURL();
		} catch (MalformedURLException | URISyntaxException ex) {
			LOGGER.error("Invalid well-known URL", ex);
			return urls;
		}

		try (InputStream is = url.openStream();
				BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
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
					try {
						metaURI = csvLocation.toURI().resolve(s);
					} catch (URISyntaxException ex) {
						metaURI = null;
						LOGGER.error("Invalid metadata URL", ex);
					}
					break;
				default:
					metaURI = URI.create(s);
				}
				try {
					if (metaURI != null) {
						urls.add(metaURI.toURL());
					}
				} catch (MalformedURLException ex) {
					LOGGER.error("Error converting metadata URI {} to URL c", metaURI, ex);
				}
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.info("Could not open {}", url);
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
	 * Try to open URL, silently fail if there is an error
	 *
	 * @param url
	 * @return
	 */
	private byte[] tryURL(URL url) {
		byte[] buffer = null;

		try (InputStream is = url.openStream();
				BufferedInputStream bis = new BufferedInputStream(is)) {
			buffer = bis.readAllBytes();
			LOGGER.info("Opened metadata from {}", url);
			return buffer;
		} catch (IOException ex) {
			LOGGER.debug("Could not open possible metadata location {}", url, ex);
		}
		return buffer;
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

		List<URL> urls = wellKnownURL();
		urls.add(specificURLExtension());
		urls.add(specificURL());
		urls.add(genericURL());

		byte[] bytes = null;

		for (URL url : urls) {
			bytes = tryURL(url);
			if (bytes != null) {
				break;
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
