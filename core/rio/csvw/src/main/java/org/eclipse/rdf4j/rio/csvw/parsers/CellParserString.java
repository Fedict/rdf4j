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
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens
 */
public class CellParserString extends CellParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(CellParserString.class);

	@Override
	protected Value parseOne(String str) {
		String lang = getLang();
		if (lang != null && XSD.STRING.equals(getDataType())) {
			return Values.literal(str, lang);
		}
		try {
			return Values.literal(str, getDataType());
		} catch (IllegalArgumentException ioe) {
			LOGGER.error("Not a valid value " + str + " " + getDataType());
			return null;
		}
	}

}
