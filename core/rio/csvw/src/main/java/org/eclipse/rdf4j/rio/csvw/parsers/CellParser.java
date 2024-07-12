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
	private String name;
	private IRI dataType;
	private String lang;
	private String defaultValue;
	private String nullValue;
	private boolean required;
	private boolean virtual = false;
	private IRI propertyIRI;
	private String valueUrl;
	private String format;
	private String decimalChar = ".";
	private String groupChar;
	private String separator;
	private boolean trim = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IRI getDataType() {
		return dataType;
	}

	public void setDataType(IRI dataType) {
		this.dataType = dataType;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getNullValue() {
		return nullValue;
	}

	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean isRequired) {
		this.required = isRequired;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean isVirtual) {
		this.virtual = isVirtual;
	}

	public IRI getPropertyIRI() {
		return propertyIRI;
	}

	/**
	 * Set property URL (predicate IRI)
	 *
	 * @param namespaces  set of namespaces
	 * @param propertyUrl the propertyUrl to set
	 */
	public void setPropertyIRI(Set<Namespace> namespaces, String propertyUrl) {
		this.propertyIRI = Values.iri(namespaces, propertyUrl);
	}

	/**
	 * Set property URL (predicate IRI) relative to document
	 *
	 * @param propertyUrl the propertyUrl to set
	 */
	public void setPropertyIRI(String propertyUrl) {
		this.propertyIRI = Values.iri("", propertyUrl);
	}

	public String getValueUrl() {
		return valueUrl;
	}

	public void setValueUrl(String valueUrl) {
		this.valueUrl = valueUrl;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getDecimalChar() {
		return decimalChar;
	}

	public void setDecimalChar(String decimalChar) {
		this.decimalChar = decimalChar;
	}

	public String getGroupChar() {
		return groupChar;
	}

	public void setGroupChar(String groupChar) {
		this.groupChar = groupChar;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public boolean isTrim() {
		return trim;
	}

	public void setTrim(boolean trim) {
		this.trim = trim;
	}

	/**
	 * Get the (possibly trimmed) value or default value
	 *
	 * @param s
	 * @return
	 */
	protected String getValueOrDefault(String s) {
		if ((s == null || s.isEmpty()) && (defaultValue != null)) {
			return defaultValue;
		}
		if (s == null) {
			return null;
		}
		if (s.equals(nullValue)) {
			return null;
		}

		return trim ? s.trim() : s;
	}

	/**
	 * Get the value from a cell
	 *
	 * @param cell
	 * @return
	 */
	public abstract Value parse(String cell);

}
