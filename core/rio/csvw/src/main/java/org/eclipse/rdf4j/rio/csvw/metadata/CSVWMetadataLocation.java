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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get metadata for a given CSV file using a known location
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataLocation implements CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataLocation.class);

	private final URL metadataURL;

	@Override
	public InputStream getMetadata() {
		if (metadataURL == null) {
			LOGGER.error("No metadata for location");
			return null;
		}

		try (InputStream is = metadataURL.openStream()) {
			LOGGER.info("Using metadata from {} ", metadataURL);
			return new ByteArrayInputStream(is.readAllBytes());
		} catch (IOException ioe) {
			LOGGER.error("Could not open metadata file {}", metadataURL);
			return null;
		}
	}

	/**
	 * Constructor
	 *
	 * @param metadataPath
	 * @throws java.net.MalformedURLException
	 */
	public CSVWMetadataLocation(Path metadataPath) throws MalformedURLException {
		this.metadataURL = metadataPath.toFile().toURI().toURL();
	}

	/**
	 * Constructor
	 *
	 * @param metadataURL
	 */
	public CSVWMetadataLocation(URL metadataURL) {
		this.metadataURL = metadataURL;
	}
}
