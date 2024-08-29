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

import java.io.InputStream;

/**
 * Empty metadata provider.
 * Mostly for testing purposes, since not providing metadata will result in relative subject/property URIs
 *
 * @author Bart Hanssens
 */
public class CSVWMetadataNone implements CSVWMetadataProvider {

	@Override
	public InputStream getMetadata() {
		return null;
	}
}
