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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
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
	private String encodedName;
	private IRI dataType;
	private String lang;
	private String defaultValue;
	private String nullValue;
	private boolean required;
	private String aboutUrl;
	private IRI propertyIRI;
	private String valueUrl;
	private String format;
	private String decimalChar = ".";
	private String groupChar;
	private String separator;
	private boolean trim = true;
	private boolean virtual = false;
	private boolean suppressed = false;

	private String aboutPlaceholder;
	private String[] aboutPlaceholders = new String[0];

	private String valuePlaceholder;
	private String[] valuePlaceholders = new String[0];

	/**
	 * Get name of the column
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get URL encoded name
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
		this.encodedName = "{" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "}";
	}

	/**
	 * Get datatype
	 *
	 * @return
	 */
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

	/**
	 * Extract placeholder name for the own column, if any
	 *
	 * @param template URI template string
	 * @return placeholder name or null
	 */
	private String getOwnPlaceholder(String template) {
		if (encodedName != null) {
			String placeholder = "{" + encodedName + "}";
			if (template.contains(placeholder)) {
				return placeholder;
			}
		}
		return null;
	}

	/**
	 * Extract placeholder names for (values of) other columns, if any
	 *
	 * @param template URI template string
	 * @return array of placeholder names
	 */
	private String[] getPlaceholders(String template) {
		Matcher matcher = PLACEHOLDERS.matcher(template);
		String ownPlaceholder = getOwnPlaceholder(template);

		Set<String> placeholders = matcher.results()
				.map(MatchResult::group)
				.filter(m -> !m.equals(ownPlaceholder))
				.collect(Collectors.toSet());
		return placeholders.toArray(String[]::new);
	}

	/**
	 * Get aboutURL
	 *
	 * @param cell
	 * @return
	 */
	public IRI getAboutUrl(String cell) {
		if (aboutUrl == null) {
			return null;
		}
		String s = aboutUrl;
		if (aboutPlaceholder != null && cell != null) {
			s = aboutUrl.replace(aboutPlaceholder, getValueOrDefault(cell));
		}
		return Values.iri(s);
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
		String s = aboutUrl;
		for (String val : aboutPlaceholders) {
			s = aboutUrl.replace(val, values.get(val));
		}
		return Values.iri(s);
	}

	/**
	 * Set aboutUrl
	 *
	 * @param aboutUrl
	 */
	public void setAboutUrl(String aboutUrl) {
		this.aboutUrl = aboutUrl;
		// check if this URL contains column placeholders
		this.aboutPlaceholder = getOwnPlaceholder(aboutUrl);
		this.aboutPlaceholders = getPlaceholders(aboutUrl);
	}

	/**
	 * Get about placeholders
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
		this.propertyIRI = Values.iri(propertyUrl);
	}

	/**
	 * Get valueURL
	 *
	 * @param cell
	 * @return
	 */
	public IRI getValueUrl(String cell) {
		if (valueUrl == null) {
			return null;
		}
		String s = valueUrl;
		if (valuePlaceholder != null && cell != null) {
			s = valueUrl.replace(valuePlaceholder, getValueOrDefault(cell));
		}
		return Values.iri(s);
	}

	/**
	 * Get valueURL with placeholders replaced with values
	 *
	 * @param values
	 * @param cell
	 * @return
	 */
	public IRI getValueUrl(Map<String, String> values, String cell) {
		if (valueUrl == null) {
			return null;
		}
		String s = valueUrl;
		if (valuePlaceholder != null && cell != null) {
			s = valueUrl.replace(encodedName, getValueOrDefault(cell));
		}
		for (String val : valuePlaceholders) {
			s = valueUrl.replace(val, values.get(val));
		}
		return Values.iri(s);
	}

	/**
	 * Set valueUrl
	 *
	 * @param valueUrl
	 */
	public void setValueUrl(String valueUrl) {
		this.valueUrl = valueUrl;
		// check if this URL contains column placeholders
		this.valuePlaceholder = getOwnPlaceholder(valueUrl);
		this.valuePlaceholders = getPlaceholders(valueUrl);
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
