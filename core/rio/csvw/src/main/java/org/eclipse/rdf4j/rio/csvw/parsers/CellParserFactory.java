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

import static org.eclipse.rdf4j.model.base.CoreDatatype.XSD.STRING;

import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 *
 * @author Bart.Hanssens
 */
public class CellParserFactory {

	private static XSD getDataType(Model model, Resource column) {
		Optional<Value> val = Models.getProperty(model, column, CSVW.DATATYPE);
		if (!val.isPresent()) {
			return XSD.STRING;
		}
		Value v = val.get();
		XSD datatype = null;

		if (v instanceof Literal) {
			datatype = XSD.valueOf(v.stringValue().toUpperCase());
		}
		if (v instanceof Resource) {
			val = Models.getProperty(model, (Resource) v, CSVW.BASE);
			if (!val.isPresent()) {
				return XSD.STRING;
			}
			v = val.get();
			datatype = XSD.valueOf(v.stringValue().toUpperCase());
		}
		if (datatype == null) {
			throw new RDFParseException("Could not parse datatype of column");
		}
		return datatype;
	}

	/**
	 * Create a CellParser based on the (JSON-LD) metadata of a column
	 *
	 * @param Model
	 * @return
	 */
	public static CellParser fromMetadata(Model model, Resource column) {
		CellParser parser;

		XSD dataType = getDataType(model, column);
		switch (dataType) {
		case STRING:
			parser = new CellParser<String>();
			break;
		case BOOLEAN:
			parser = new CellParser<Boolean>();
			break;
		case INTEGER:
			parser = new CellParser<Integer>();
			break;
		default:
			parser = new CellParser<String>();
			break;
		}
		return parser;
	}
}
