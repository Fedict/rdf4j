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
	public Value parse(String cell) {
		String s = getValueOrDefault(cell);

		if (s != null && groupChar != null) {
			s = s.replace(groupChar, "");
		}

		// always use a '.' in RDF, not the European-style ','
		if (s != null && !decimalChar.equals(".")) {
			s = s.replace(decimalChar, ".");
		}

		return Values.literal(s, dataType);
	}

}
