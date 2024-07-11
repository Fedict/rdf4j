/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for CSV on the Web
 *
 * @author Bart Hanssens
 * @see <a href="https://csvw.org/">CSV on the Web</a>
 */
public class CSVW {
	/**
	 * The CSVW namespace: http://www.w3.org/ns/csvw#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/csvw#";

	/**
	 * Recommended prefix for the namespace: "csvw"
	 */
	public static final String PREFIX = "csvw";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes

	// Properties
	/** csvw:aboutUrl */
	public static final IRI ABOUT_URL;

	/** csvw:base */
	public static final IRI BASE;

	/** csvw:columns */
	public static final IRI COLUMN;

	/** csvw:datatype */
	public static final IRI DATATYPE;

	/** csvw:decimalChar */
	public static final IRI DECIMAL_CHAR;

	/** csvw:default */
	public static final IRI DEFAULT;

	/** csvw:delimiter */
	public static final IRI DELIMITER;

	/** csvw:dialect */
	public static final IRI DIALECT;

	/** csvw:format */
	public static final IRI FORMAT;

	/** csvw:groupChar */
	public static final IRI GROUP_CHAR;

	/** csvw:header */
	public static final IRI HEADER;

	/** csvw:lang */
	public static final IRI LANG;

	/** csvw:name */
	public static final IRI NAME;

	/** csvw:propertyUrl */
	public static final IRI PROPERTY_URL;

	/** csvw:required */
	public static final IRI REQUIRED;

	/** csvw:tableSchema */
	public static final IRI TABLE_SCHEMA;

	/** csvw:tables */
	public static final IRI TABLES;

	/** csvw:title */
	public static final IRI TITLE;

	/** csvw:url */
	public static final IRI URL;

	/** csvw:valueUrl */
	public static final IRI VALUE_URL;

	/** csvw:virtual */
	public static final IRI VIRTUAL;

	static {
		ABOUT_URL = Vocabularies.createIRI(NAMESPACE, "aboutUrl");
		BASE = Vocabularies.createIRI(NAMESPACE, "base");
		COLUMN = Vocabularies.createIRI(NAMESPACE, "column");
		DATATYPE = Vocabularies.createIRI(NAMESPACE, "datatype");
		DECIMAL_CHAR = Vocabularies.createIRI(NAMESPACE, "decimalChar");
		DEFAULT = Vocabularies.createIRI(NAMESPACE, "default");
		DELIMITER = Vocabularies.createIRI(NAMESPACE, "delimiter");
		DIALECT = Vocabularies.createIRI(NAMESPACE, "dialect");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "format");
		GROUP_CHAR = Vocabularies.createIRI(NAMESPACE, "groupChar");
		HEADER = Vocabularies.createIRI(NAMESPACE, "header");
		LANG = Vocabularies.createIRI(NAMESPACE, "lang");
		NAME = Vocabularies.createIRI(NAMESPACE, "name");
		PROPERTY_URL = Vocabularies.createIRI(NAMESPACE, "propertyUrl");
		REQUIRED = Vocabularies.createIRI(NAMESPACE, "required");
		TABLE_SCHEMA = Vocabularies.createIRI(NAMESPACE, "tableSchema");
		TABLES = Vocabularies.createIRI(NAMESPACE, "tables");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		URL = Vocabularies.createIRI(NAMESPACE, "url");
		VALUE_URL = Vocabularies.createIRI(NAMESPACE, "valueUrl");
		VIRTUAL = Vocabularies.createIRI(NAMESPACE, "virtual");
	}
}
