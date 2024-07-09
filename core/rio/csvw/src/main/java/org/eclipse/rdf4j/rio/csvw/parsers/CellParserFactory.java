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
package org.eclipse.rdf4j.rio.csvw.parsers;

import org.eclipse.rdf4j.model.IRI;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserFactory {
	/**
	 * Create a new CellParser based on datatype
	 * @param datatype
	 * @return 
	 */
	public static CellParser create(IRI datatype) {
		CellParser p = new CellParser();
		p.setDataType(datatype);
		return p;
	}
}
