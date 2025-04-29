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
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserDate extends CellParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(CellParserDate.class);

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
			try {
				TemporalAccessor temp = formatter.parse(str);
				return Values.literal(temp);
			} catch (DateTimeParseException dtpe) {
				LOGGER.error("Not a valid value " + str + " " + getDataType());
				return null;
			}
		}
		try {
			return Values.literal(str, getDataType());
		} catch (IllegalArgumentException ioe) {
			LOGGER.error("Not a valid value " + str + " " + getDataType());
			return null;
		}
	}
}
