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
import java.nio.charset.StandardCharsets;

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

	private ByteArrayInputStream buffer;

	@Override
	public InputStream getMetadata() {
		return buffer;
	}

	/**
	 * Find by adding metadata.json as file extension
	 *
	 * @param csvFile
	 */
	public void findByExtension(URI csvFile) {
		String s = csvFile.toString();
		if (s.endsWith(CSV)) {
			s = s.substring(0, s.length() - CSV.length());
		}
		URI metaURI = URI.create(s + METADATA_EXT);
		buffer = openURI(metaURI);
	}

	/**
	 * Find by trying to get the csv-metadata.json in the path
	 *
	 * @param csvFile
	 */
	public void findInPath(URI csvFile) {
		URI metaURI = csvFile.resolve(METADATA_CSV);
		buffer = openURI(metaURI);
	}

	/**
	 * Try reading the well-known location
	 *
	 * @param csvFile
	 */
	public void findByWellKnown(URI csvFile) {
		URI wellKnown = csvFile.resolve(WELL_KNOWN);

		try (InputStream is = wellKnown.toURL().openStream();
				BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
			URI metaURI;
			String line = r.readLine();

			while (line != null) {
				String s = line.replaceFirst("\\{\\+?url\\}", csvFile.toString());
				if (s.isBlank()) {
					continue;
				}
				switch (line.charAt(0)) {
				case '?':
					metaURI = URI.create(line + s);
					break;
				case '/':
					metaURI = csvFile.resolve(s);
					break;
				default:
					metaURI = URI.create(s);
				}
				try (InputStream meta = metaURI.toURL().openStream()) {
					buffer = new ByteArrayInputStream(meta.readAllBytes());
				} catch (IOException ioe) {
					LOGGER.debug("Could not open {}", metaURI);
				}
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.info("Could not open {}", wellKnown);
		}
	}

	/**
	 * Try different ways to obtain CSVW metadata file
	 *
	 * @param csvFile
	 */
	public void find(URI csvFile) {
		buffer = null;
		findByExtension(csvFile);
		if (buffer == null) {
			findInPath(csvFile);
		}
		if (buffer == null) {
			findByWellKnown(csvFile);
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
