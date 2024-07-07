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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 *
 * @author Bart.Hanssens
 */
public class Parser {
	private String defaultValue;
	private boolean isRequired;
	private String format;
	private String propertyUrl;
	private String valueUrl;
	private String separator;

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @param isRequired the isRequired to set
	 */
	public void setIsRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * @return the propertyUrl
	 */
	public String getPropertyUrl() {
		return propertyUrl;
	}

	/**
	 * @param propertyUrl the propertyUrl to set
	 */
	public void setPropertyUrl(String propertyUrl) {
		this.propertyUrl = propertyUrl;
	}

	/**
	 * @return the valueUrl
	 */
	public String getValueUrl() {
		return valueUrl;
	}

	/**
	 * @param valueUrl the valueUrl to set
	 */
	public void setValueUrl(String valueUrl) {
		this.valueUrl = valueUrl;
	}

	/**
	 * @return the separator
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * @param separator the separator to set
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public Value parse(String cell) {
		if (cell == null || cell.isEmpty()) {
			return Values.literal(defaultValue);
		}
		return Values.literal(cell);
	}

}
