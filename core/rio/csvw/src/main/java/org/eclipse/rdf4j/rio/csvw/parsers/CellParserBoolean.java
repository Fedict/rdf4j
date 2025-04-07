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
public class CellParserBoolean extends CellParser {
	private String valueTrue;
	private String valueFalse;

	@Override
	public void setFormat(String format) {
		String[] values = format.split("\\|");
		valueTrue = values[0];
		valueFalse = values[1];
	}

	@Override
	public Value parse(String cell) {
		String s = getValueOrDefault(cell);
		if (s == null || s.isEmpty()) {
			return null;
		}
		return Values.literal(valueTrue.equals(s) ? "true" : "false", getDataType());
	}

}
