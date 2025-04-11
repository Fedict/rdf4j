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

	private URL csvLocation;
	private ByteArrayInputStream buffer;

	/**
	 *
	 * @return
	 */
	public URL getCsvLocation() {
		return csvLocation;
	}

	/**
	 *
	 * @param location
	 */
	public void setCsvLocation(URL location) {
		this.csvLocation = location;
	}

	@Override
	public InputStream getMetadata() {
		if (csvLocation == null) {
			LOGGER.error("Location of CSV file not known, metadata location cannot be determined");
			return null;
		}
		buffer = null;

		try {
			checkSpecific();
			if (buffer == null) {
				checkSpecificAfterExt();
			}
			if (buffer == null) {
				checkGeneric();
			}
			if (buffer == null) {
				checkWellKnown();
			}
		} catch (MalformedURLException | URISyntaxException e) {
			LOGGER.error("Error in checking URL", e);
		}
		return buffer;
	}

	/**
	 * Check if there is a "-metadata.json" file relative to the location of the CSV file
	 *
	 * @throws MalformedURLException
	 */
	public void checkSpecific() throws MalformedURLException {
		String s = csvLocation.toString();
		if (s.endsWith(CSV)) {
			s = s.substring(0, s.length() - CSV.length());
		}

		URL metaURI = new URL(s + METADATA_EXT);
		buffer = openURL(metaURI);
	}

	/**
	 * Check if there is a "-metadata.json" file relative to the location of the CSV file
	 *
	 * @throws MalformedURLException
	 */
	public void checkSpecificAfterExt() throws MalformedURLException {
		String s = csvLocation.toString();
		URL metaURI = new URL(s + METADATA_EXT);
		buffer = openURL(metaURI);
	}

	/**
	 * Check if there is a generic "csv-metadata.json" on the server
	 *
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public void checkGeneric() throws URISyntaxException, MalformedURLException {
		URL url = csvLocation.toURI().resolve(METADATA_CSV).toURL();
		buffer = openURL(url);
	}

	/**
	 * Check if there is a file in a ".well-known" location on the server
	 *
	 * @throws java.net.URISyntaxException
	 * @throws java.net.MalformedURLException
	 */
	public void checkWellKnown() throws URISyntaxException, MalformedURLException {
		URL url = csvLocation.toURI().resolve(WELL_KNOWN).toURL();

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
					metaURI = csvLocation.toURI().resolve(s);
					break;
				default:
					metaURI = URI.create(s);
				}
				buffer = openURL(metaURI.toURL());
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.info("Could not open {}", url);
		}
	}

	/**
	 * Open URI as input stream
	 *
	 * @param uri
	 * @return
	 */
	private ByteArrayInputStream openURL(URL url) {
		try (InputStream is = url.openStream()) {
			return new ByteArrayInputStream(is.readAllBytes());
		} catch (IOException ioe) {
			LOGGER.debug("Could not open {}", url);
			return null;
		}
	}
}
