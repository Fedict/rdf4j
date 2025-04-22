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
import java.time.temporal.TemporalAccessor;

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
		if (format.contains("T") && !format.contains("'T'")) {
			format = format.replace("T", "'T'");
		}
		formatter = DateTimeFormatter.ofPattern(format);
	}

	@Override
	protected Value parseOne(String str) {
		if (formatter != null) {
			TemporalAccessor temp = formatter.parse(str);
			return Values.literal(temp);
		}
		return Values.literal(str, getDataType());
	}
}
