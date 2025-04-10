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
import java.net.URI;

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

	private URI csvLocation;
	private ByteArrayInputStream buffer;

	/**
	 *
	 * @return
	 */
	public URI getCsvLocation() {
		return csvLocation;
	}

	/**
	 *
	 * @param location
	 */
	public void setCsvLocation(URI location) {
		this.csvLocation = location;
	}

	@Override
	public InputStream getMetadata() {
		if (csvLocation == null) {
			LOGGER.error("Location of CSV file not known, metadata location cannot be determined");
			return null;
		}
		buffer = null;

		checkSpecific();
		if (buffer == null) {
			checkGeneric();
		}
		if (buffer == null) {
			checkWellKnown();
		}
		return buffer;
	}

	/**
	 * Check if there is a "-metadata.json" file relative to the location of the CSV file
	 */
	public void checkSpecific() {
		String s = csvLocation.toString();
		if (s.endsWith(CSV)) {
			s = s.substring(0, s.length() - CSV.length());
		}
		URI metaURI = URI.create(s + METADATA_EXT);
		buffer = openURI(metaURI);
	}

	/**
	 * Check if there is a generic "csv-metadata.json" on the server
	 */
	public void checkGeneric() {
		URI metaURI = csvLocation.resolve(METADATA_CSV);
		buffer = openURI(metaURI);
	}

	/**
	 * Check if there is a file in a ".well-known" location on the server
	 */
	public void checkWellKnown() {
		URI wellKnown = csvLocation.resolve(WELL_KNOWN);

		try (InputStream is = wellKnown.toURL().openStream();
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
					metaURI = csvLocation.resolve(s);
					break;
				default:
					metaURI = URI.create(s);
				}
				buffer = openURI(metaURI);
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.info("Could not open {}", wellKnown);
		}
	}

	/**
	 * Open URI as input stream
	 *
	 * @param uri
	 * @return
	 */
	private ByteArrayInputStream openURI(URI uri) {
		try (InputStream is = uri.toURL().openStream()) {
			return new ByteArrayInputStream(is.readAllBytes());
		} catch (IOException ioe) {
			LOGGER.debug("Could not open {}", uri);
			return null;
		}
	}
}
