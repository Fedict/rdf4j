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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get metadata for a given CSV file using a known location
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataPath extends CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataPath.class);

	private final Path metadataPath;

	@Override
	public InputStream getMetadata() {
		if (metadataPath == null) {
			LOGGER.error("Meta data path is null");
		}

		try (InputStream is = Files.newInputStream(metadataPath);
				BufferedInputStream bis = new BufferedInputStream(is)) {
			LOGGER.info("Using metadata from path {} ", metadataPath);
			return new ByteArrayInputStream(bis.readAllBytes());
		} catch (IOException ioe) {
			LOGGER.error("Could not open metadata from path {}", metadataPath);
		}
		return null;
	}

	/**
	 * Constructor
	 *
	 * @param metadataPath
	 */
	public CSVWMetadataPath(Path metadataPath) {
		this.metadataPath = metadataPath;
	}
}
