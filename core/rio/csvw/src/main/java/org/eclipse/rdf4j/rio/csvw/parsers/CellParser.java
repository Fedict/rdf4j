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

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

/**
 *
 * @author Bart Hanssens
 */
public abstract class CellParser {
	protected String name;
	protected IRI dataType;
	protected String lang;
	protected String defaultValue;
	protected boolean isRequired;
	protected IRI propertyIRI;
	protected String valueUrl;
	protected String format;
	protected String decimalChar;
	protected String groupChar;
	protected String separator;

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param dataType
	 */
	public void setDataType(IRI dataType) {
		this.dataType = dataType;
	}

	/**
	 * Set language code
	 *
	 * @param lang language code
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

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
	 * @return the propertyUrl as IRI
	 */
	public IRI getPropertyIRI() {
		return propertyIRI;
	}

	/**
	 * Set property URL (predicate IRI)
	 *
	 * @param namespaces  set of namespaces
	 * @param propertyUrl the propertyUrl to set
	 */
	public void setPropertyURL(Set<Namespace> namespaces, String propertyUrl) {
		this.propertyIRI = Values.iri(namespaces, propertyUrl);
	}

	/**
	 * Set property URL (predicate IRI) relative to document
	 *
	 * @param propertyUrl the propertyUrl to set
	 */
	public void setPropertyURL(String propertyUrl) {
		this.propertyIRI = Values.iri("", propertyUrl);
	}

	/**
	 * @return the valueUrl
	 */
	public String getValueURL() {
		return valueUrl;
	}

	/**
	 * @param valueUrl the valueUrl to set
	 */
	public void setValueURL(String valueUrl) {
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

	/**
	 * @return the decimal character
	 */
	public String getDecimalChar() {
		return decimalChar;
	}

	/**
	 * @param decimalChar the decimal character to set
	 */
	public void setDecimalChar(String decimalChar) {
		this.decimalChar = decimalChar;
	}

	/**
	 * @return the group character
	 */
	public String getGroupChar() {
		return groupChar;
	}

	/**
	 * @param groupChar the group character to set
	 */
	public void setGroupChar(String groupChar) {
		this.groupChar = groupChar;
	}

	/**
	 * @param format
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	protected String getValueOrDefault(String s) {
		if ((s == null || s.isEmpty()) && (defaultValue != null)) {
			return defaultValue;
		}
		return s;
	}

	/**
	 * Get the value from a cell
	 *
	 * @param cell
	 * @return
	 */
	public abstract Value parse(String cell);

}
