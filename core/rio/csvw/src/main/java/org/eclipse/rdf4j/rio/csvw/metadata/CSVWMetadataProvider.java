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
 * Provide the JSON metadata for parsing the CSV data
 *
 * @author Bart Hanssens
 */
public interface CSVWMetadataProvider {

	/**
	 * Get the metadata as inputstream
	 *
	 * @return
	 */
	public abstract InputStream getMetadata();
}
