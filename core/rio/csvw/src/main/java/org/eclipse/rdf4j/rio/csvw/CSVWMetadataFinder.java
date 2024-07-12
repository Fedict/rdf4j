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
package org.eclipse.rdf4j.rio.csvw;

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
public class CSVWMetadataFinder {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataFinder.class);

	private static final String WELL_KNOWN = "/.well-known/csvm";
	private static final String METADATA_EXT = "-metadata.json";
	private static final String METADATA_CSV = "csv-metadata.json";
	private static final String CSV = ".csv";

	/**
	 * Open URI as input stream
	 * 
	 * @param uri
	 * @return 
	 */
	private static InputStream openURI(URI uri) {
		try (InputStream is = uri.toURL().openStream()) {
			return new ByteArrayInputStream(is.readAllBytes());
		} catch (IOException ioe) {
			LOGGER.debug("Could not open {}", uri);
			return null;
		}
	}
	/**
	 * Find by adding metadata.json as file extension
	 *
	 * @param csvFile
	 * @return inputstream or null
	 */
	public static InputStream findByExtension(URI csvFile) {
		String s = csvFile.toString();
		if (s.endsWith(CSV)) {
			s = s.substring(0, s.length() - CSV.length());
		}
		URI metaURI = URI.create(s + METADATA_EXT);
		return openURI(metaURI);
	}
	
	/**
	 * Find by trying to get the csv-metadata.json in the path
	 *
	 * @param csvFile
	 * @return inputstream or null
	 */
	public static InputStream findInPath(URI csvFile) {
		URI metaURI = csvFile.resolve(METADATA_CSV);
		return openURI(metaURI);
	}

	/**
	 * Try reading the well-known location
	 *
	 * @param csvFile
	 * @return URI or null
	 */
	public static InputStream findByWellKnown(URI csvFile) {
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
					return new ByteArrayInputStream(meta.readAllBytes());
				} catch (IOException ioe) {
					LOGGER.debug("Could not open {}", metaURI);
				}
				line = r.readLine();
			}
		} catch (IOException ioe) {
			LOGGER.info("Could not open {}", wellKnown);
		}
		return null;
	}
}
