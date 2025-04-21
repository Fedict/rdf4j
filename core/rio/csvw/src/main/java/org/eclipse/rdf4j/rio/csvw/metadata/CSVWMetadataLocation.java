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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get metadata for a given CSV file using a known location
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataLocation extends CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataLocation.class);

	private final URL metadataURL;

	@Override
	public InputStream getMetadata() {
		if (metadataURL == null) {
			LOGGER.error("Meta data path is null");
			return null;
		}
		try {
			return new ByteArrayInputStream(CSVWMetadataUtil.tryURI(metadataURL.toURI()));
		} catch (URISyntaxException ex) {
			LOGGER.error("Invalid URL {}", metadataURL, ex);
		}
		return null;
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
