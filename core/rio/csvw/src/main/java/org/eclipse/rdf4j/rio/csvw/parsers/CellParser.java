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

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

/**
 *
 * @author Bart Hanssens
 */
public abstract class CellParser {
	private static final Pattern PLACEHOLDERS = Pattern.compile("(\\{#?_?[^\\}]+\\})");

	private String name;
	private IRI dataType;
	private String lang;
	private String defaultValue;
	private String nullValue;
	private boolean required;
	private IRI propertyIRI;
	private String valueUrl;
	private String format;
	private String decimalChar = ".";
	private String groupChar;
	private String separator;
	private boolean trim = true;
	private boolean virtual = false;
	private String[] propPlaceholder;
	private String[] valPlaceholder;

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

	/**
	 * Get default value
	 * 
	 * @return 
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Set default value
	 * 
	 * @param defaultValue 
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Get NULL value
	 * 
	 * @return 
	 */
	public String getNullValue() {
		return nullValue;
	}

	/**
	 * Set NULL value
	 * 
	 * @param nullValue 
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean isRequired) {
		this.required = isRequired;
	}

	/**
	 * IS virtual table ?
	 * 
	 * @return 
	 */
	public boolean isVirtual() {
		return virtual;
	}

	/**
	 * Set virtual table
	 * 
	 * @param virtual 
	 */
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public IRI getPropertyIRI() {
		return propertyIRI;
	}

	/**
	 * Extract placeholders (if any)
	 * 
	 * @param template URI template string
	 * @return array of placeholders 
	 */
	private String[] extractPlaceholders(String template) {
		Matcher matcher = PLACEHOLDERS.matcher(template);
		if (matcher.find()) {
			int matches = matcher.groupCount();
			String[] placeholders = new String[matches];
			for (int i = 0; i < matches; i++) {
				placeholders[i] = matcher.group(i + 1);
			}
			return placeholders;
		}
		return null;
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

	/**
	 * Get valueURL
	 * 
	 * @return 
	 */
	public String getValueUrl() {
		return valueUrl;
	}

	/**
	 * Set valueUrl
	 * 
	 * @param valueUrl 
	 */
	public void setValueUrl(String valueUrl) {
		this.valueUrl = valueUrl;
		this.valPlaceholder = extractPlaceholders(valueUrl);
	}

	/**
	 * Get format
	 * 
	 * @return 
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Set format
	 * 
	 * @param format 
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Get decimal character
	 * 
	 * @return 
	 */
	public String getDecimalChar() {
		return decimalChar;
	}

	/**
	 * Set decimal character
	 * 
	 * @param decimalChar 
	 */
	public void setDecimalChar(String decimalChar) {
		this.decimalChar = decimalChar;
	}

	/**
	 * Get group character
	 * 
	 * @return 
	 */
	public String getGroupChar() {
		return groupChar;
	}

	/**
	 * Set group character
	 * 
	 * @param groupChar 
	 */
	public void setGroupChar(String groupChar) {
		this.groupChar = groupChar;
	}

	/**
	 * Get separator character
	 * 
	 * @return 
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * Set separator character
	 * 
	 * @param separator 
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Is trim enabled
	 * 
	 * @return 
	 */
	public boolean isTrim() {
		return trim;
	}

	/**
	 * Set if value needs to be trimmed
	 * 
	 * @param trim 
	 */
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
