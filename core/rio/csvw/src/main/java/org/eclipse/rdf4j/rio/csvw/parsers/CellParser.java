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

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.csvw.CSVWUtil;

/**
 *
 * @author Bart Hanssens
 */
public abstract class CellParser {
	private static final Pattern PLACEHOLDERS = Pattern.compile("\\{#?(_?[^\\}]+)\\}");

	private Set<Namespace> namespaces;
	private String name;
	private String encodedName;
	private IRI dataType;
	private String lang;
	private String defaultValue;
	private String nullValue;
	private boolean required;
	private String aboutUrl;
	private String propertyUrl;
	private String valueUrl;
	private String format;
	private String decimalChar = ".";
	private String groupChar;
	private String separator;
	private boolean trim = true;
	private boolean virtual = false;
	private boolean suppressed = false;

	private String[] aboutPlaceholders = new String[0];
	private String[] propertyPlaceholders = new String[0];
	private String[] valuePlaceholders = new String[0];

	public void setNamespaces(Set<Namespace> namespaces) {
		this.namespaces = namespaces;
	}

	/**
	 * Get name of the column
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get URL-encoded name
	 *
	 * @return encoded name
	 */
	public String getNameEncoded() {
		return encodedName;
	}

	/**
	 * Set name of the column
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
		this.encodedName = CSVWUtil.encode(name);
	}

	/**
	 * Get datatype
	 *
	 * @return
	 */
	public IRI getDataType() {
		return dataType;
	}

	/**
	 * Set datatype IRI
	 *
	 * @param dataType
	 */
	public void setDataType(IRI dataType) {
		this.dataType = dataType;
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
	 * Set if output needs to be suppressed
	 *
	 * @return
	 */
	public boolean isSuppressed() {
		return suppressed;
	}

	/**
	 * Set if output needs to be suppressed
	 *
	 * @param suppressed
	 */
	public void setSuppressed(boolean suppressed) {
		this.suppressed = suppressed;
	}

	/**
	 * Get language
	 *
	 * @return
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * Set language
	 *
	 * @param lang
	 */
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

	/**
	 * Set
	 *
	 * @param isRequired
	 */
	public void setRequired(boolean isRequired) {
		this.required = isRequired;
	}

	/**
	 * Does this a cell belongs to a virtual column
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

	/**
	 * Replace placeholders in URL with values
	 *
	 * @param str
	 * @param placeholders
	 * @param values
	 * @return
	 */
	private IRI replacePlaceholders(String str, String[] placeholders, Map<String, String> values) {
		for (String p : placeholders) {
			String val = values.get(p);
			if (val != null) {
				str = str.replace("{" + p + "}", val)
						.replace("{#" + p + "}", "#" + val);
			}
		}
		if (!str.startsWith("http") && namespaces != null) {
			if (!str.contains(":")) {
				str = ":" + (str.startsWith("#") ? str.substring(1) : str);
			}
			return Values.iri(namespaces, str);
		}
		return Values.iri(str);
	}

	/**
	 * Extract placeholder names for (values of) other columns, if any
	 *
	 * @param template URI template string
	 * @return array of placeholder names
	 */
	private String[] getPlaceholders(String template) {
		Matcher matcher = PLACEHOLDERS.matcher(template);

		Set<String> placeholders = matcher.results()
				.map(m -> m.group(1))
				.collect(Collectors.toSet());
		return placeholders.toArray(String[]::new);
	}

	/**
	 * Get aboutURL with placeholders replaced with values
	 *
	 * @param values
	 * @return
	 */
	public IRI getAboutUrl(Map<String, String> values) {
		if (aboutUrl == null) {
			return null;
		}
		return replacePlaceholders(aboutUrl, aboutPlaceholders, values);
	}

	/**
	 * Set aboutURL
	 *
	 * @param aboutUrl
	 */
	public void setAboutUrl(String aboutUrl) {
		this.aboutUrl = aboutUrl;
		aboutPlaceholders = getPlaceholders(aboutUrl);
	}

	/**
	 * Get aboutURL placeholders
	 *
	 * @return
	 */
	public String[] getAboutPlaceholders() {
		return aboutPlaceholders;
	}

	/**
	 * Get propertyUrl as IRI
	 *
	 * @return
	 */
	public IRI getPropertyUrl() {
		if (propertyUrl == null) {
			return null;
		}
		if (!propertyUrl.startsWith("http") && namespaces != null) {
			return Values.iri(namespaces, propertyUrl);
		}
		return Values.iri(propertyUrl);
	}

	/**
	 * Get propertyUrl as IRI, replacing placeholders with values
	 *
	 * @param values
	 * @return
	 */
	public IRI getPropertyUrl(Map<String, String> values) {
		if (propertyUrl == null) {
			return null;
		}
		return replacePlaceholders(propertyUrl, propertyPlaceholders, values);
	}

	/**
	 *
	 * @param propertyUrl
	 */
	public void setPropertyUrl(String propertyUrl) {
		if (propertyUrl == null) {
			throw new IllegalArgumentException();
		}
		this.propertyUrl = propertyUrl;
		propertyPlaceholders = getPlaceholders(propertyUrl);
	}

	/**
	 * Get propertyURL placeholders
	 *
	 * @return
	 */
	public String[] getPropertyPlaceholders() {
		return propertyPlaceholders;
	}

	/**
	 * Get valueURL with placeholders replaced with values
	 *
	 * @param values
	 * @return
	 */
	public IRI getValueUrl(Map<String, String> values) {
		if (valueUrl == null) {
			return null;
		}
		return replacePlaceholders(valueUrl, valuePlaceholders, values);
	}

	/**
	 * Set valueUrl
	 *
	 * @param valueUrl
	 */
	public void setValueUrl(String valueUrl) {
		this.valueUrl = valueUrl;
		valuePlaceholders = getPlaceholders(valueUrl);
	}

	/**
	 * Get value placeholders
	 *
	 * @return
	 */
	public String[] getValuePlaceholders() {
		return valuePlaceholders;
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
		if (s == null || (nullValue != null && s.equals(nullValue))) {
			return null;
		}
		if (trim) {
			s = s.trim();
		}
		return s.isEmpty() ? null : s;
	}

	protected abstract Value parseOne(String str);

	/**
	 * Get the value from a cell
	 *
	 * @param cell
	 * @return
	 */
	public Value parse(String cell) {
		String str = getValueOrDefault(cell);
		if (str == null) {
			return null;
		}
		return parseOne(str);
	}

	/**
	 * Get multiple values from a cell with a separator in it
	 *
	 * @param cell
	 * @return
	 */
	public Value[] parseMultiple(String cell) {
		String str = getValueOrDefault(cell);
		if (str == null) {
			return null;
		}
		String[] parts = str.split(getSeparator());
		Value[] values = new Value[parts.length];
		for (int i = 0; i < parts.length; i++) {
			values[i] = parseOne(parts[i]);
		}
		return values;
	}

}
