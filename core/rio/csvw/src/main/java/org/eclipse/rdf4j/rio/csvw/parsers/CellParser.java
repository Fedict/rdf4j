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
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 *
 * @author Bart Hanssens
 */
public class CellParser {
	private String name;
	protected IRI dataType;
	protected String defaultValue;
	private boolean isRequired;
	private IRI propertyIRI;
	private String valueUrl;
	private String format;
	private String separator;

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
	 * @param format
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Get the value from a cell
	 *
	 * @param cell
	 * @return
	 */
	public Value parse(String cell) {
		String s = cell;
		if ((s == null || s.isEmpty()) && (defaultValue != null)) {
			s = defaultValue;
		}
		if (valueUrl != null && s != null) {
			return Values.iri(valueUrl.replace("{" + name + "}", s));
		}
		System.err.println(s);
		System.err.println(dataType);
		return Values.literal(s, dataType);
	}

}
