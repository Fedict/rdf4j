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

import java.time.format.DateTimeFormatter;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserDate extends CellParser {
	private DateTimeFormatter formatter;

	/**
	 * @param format
	 */
	@Override
	public void setFormat(String format) {
		super.setFormat(format);
		formatter = DateTimeFormatter.ofPattern(format);
	}

	@Override
	public Value parse(String cell) {
		String s = cell;
		if ((s == null || s.isEmpty()) && (defaultValue != null)) {
			s = defaultValue;
		}
		if (formatter != null) {
			s = DateTimeFormatter.ISO_DATE.format(formatter.parse(s));
		}
		return Values.literal(s, dataType);
	}

}
