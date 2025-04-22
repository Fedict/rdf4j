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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserDecimal extends CellParser {

	@Override
	protected Value parseOne(String str) {
		if (getGroupChar() != null) {
			str = str.replace(getGroupChar(), "");
		}

		// always use a '.' in RDF, not the European-style ','
		if (!getDecimalChar().equals(".")) {
			str = str.replace(getDecimalChar(), ".");
		}
		String end = str.substring(str.length() - 2);
		if (end.equals("%") || end.equals("‰")) {
			String tmp = str.substring(0, str.length() - 2);
			int factor = end.equals("%") ? 100 : 1000;
			str = String.valueOf(Double.parseDouble(tmp) / factor);
		}
		return Values.literal(str, getDataType());
	}

}
