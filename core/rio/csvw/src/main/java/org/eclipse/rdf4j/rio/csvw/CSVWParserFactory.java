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

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for CSV on the Web parsers.
 *
 * @author Bart Hanssens
 *
 * @since 5.1.0
 */
public class CSVWParserFactory implements RDFParserFactory {
	/**
	 * Returns {@link RDFFormat#CSVW}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.CSVW;
	}

	/**
	 * Returns a new instance of {@link HDTParser}.
	 */
	@Override
	public CSVWParser getParser() {
		return new CSVWParser();
	}
}
