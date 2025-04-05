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
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserFactory {
	/**
	 * Create a new CellParser based on datatype
	 *
	 * @param datatype
	 * @return
	 */
	public static CellParser create(IRI datatype) {
		if (datatype == null) {
			return null;
		}

		CellParser p;

		XSD xsdType;
		try {
			xsdType = XSD.valueOf(datatype.getLocalName().toUpperCase());
		} catch (IllegalArgumentException ioe) {
			xsdType = XSD.STRING;
		}

		switch (xsdType) {
		case BOOLEAN:
			p = new CellParserBoolean();
			break;
		case INTEGER:
		case INT:
		case SHORT:
		case LONG:
			p = new CellParserInteger();
			break;
		case FLOAT:
		case DOUBLE:
			p = new CellParserDecimal();
			p.setDecimalChar(".");
			break;
		case DATE:
		case DATETIME:
		case TIME:
			p = new CellParserDate();
			break;
		default:
			p = new CellParserString();
		}
		p.setDataType(datatype);
		return p;
	}
}
