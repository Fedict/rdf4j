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

	/** csvw:Dialect */
	public static final IRI DIALECT;

	/** csvw:Row */
	public static final IRI ROW;

	/** csvw:Schema */
	public static final IRI SCHEMA;

	/** csvw:Table */
	public static final IRI TABLE;

	/** csvw:TableGroup */
	public static final IRI TABLE_GROUP;

	// Properties
	/** csvw:aboutUrl */
	public static final IRI ABOUT_URL;

	/** csvw:base */
	public static final IRI BASE;

	/** csvw:columns */
	public static final IRI COLUMN;

	/** csvw:commentPrefix */
	public static final IRI COMMENT_PREFIX;

	/** csvw:datatype */
	public static final IRI DATATYPE;

	/** csvw:decimalChar */
	public static final IRI DECIMAL_CHAR;

	/** csvw:default */
	public static final IRI DEFAULT;

	/** csvw:delimiter */
	public static final IRI DELIMITER;

	/** csvw:describes */
	public static final IRI DESCRIBES;

	/** csvw:doubleQuote */
	public static final IRI DOUBLE_QUOTE;

	/** csvw:encoding */
	public static final IRI ENCODING;

	/** csvw:format */
	public static final IRI FORMAT;

	/** csvw:groupChar */
	public static final IRI GROUP_CHAR;

	/** csvw:dialect */
	public static final IRI HAS_DIALECT;

	/** csvw:header */
	public static final IRI HEADER;

	/** csvw:headerRowCount */
	public static final IRI HEADER_ROW_COUNT;

	/** csvw:lang */
	public static final IRI LANG;

	/** csvw:lineTerminators */
	public static final IRI LINE_TERMINATORS;

	/** csvw:name */
	public static final IRI NAME;

	/** csvw:note */
	public static final IRI NOTE;

	/** csvw:null */
	public static final IRI NULL;

	/** csvw:ordered */
	public static final IRI ORDERED;

	/** csvw:propertyUrl */
	public static final IRI PROPERTY_URL;

	/** csvw:quoteChar */
	public static final IRI QUOTE_CHAR;

	/** csvw:required */
	public static final IRI REQUIRED;

	/** csvw:row */
	public static final IRI HAS_ROW;

	/** csvw:rownum */
	public static final IRI ROWNUM;

	/** csvw:separator */
	public static final IRI SEPARATOR;

	/** csvw:skipColumns */
	public static final IRI SKIP_COLUMNS;

	/** csvw:skipInitialSpace */
	public static final IRI SKIP_INITIAL_SPACE;

	/** csvw:skipRows */
	public static final IRI SKIP_ROWS;

	/** csvw:suppressOutput */
	public static final IRI SUPPRESS_OUTPUT;

	/** csvw:tableSchema */
	public static final IRI TABLE_SCHEMA;

	/** csvw:table */
	public static final IRI HAS_TABLE;

	/** csvw:textDirection */
	public static final IRI TEXT_DIRECTION;

	/** csvw:title */
	public static final IRI TITLE;

	/** csvw:trim */
	public static final IRI TRIM;

	/** csvw:url */
	public static final IRI URL;

	/** csvw:valueUrl */
	public static final IRI VALUE_URL;

	/** csvw:virtual */
	public static final IRI VIRTUAL;

	static {
		DIALECT = Vocabularies.createIRI(NAMESPACE, "Dialect");
		ROW = Vocabularies.createIRI(NAMESPACE, "Row");
		SCHEMA = Vocabularies.createIRI(NAMESPACE, "Schema");
		TABLE = Vocabularies.createIRI(NAMESPACE, "Table");
		TABLE_GROUP = Vocabularies.createIRI(NAMESPACE, "TableGroup");

		ABOUT_URL = Vocabularies.createIRI(NAMESPACE, "aboutUrl");
		BASE = Vocabularies.createIRI(NAMESPACE, "base");
		COLUMN = Vocabularies.createIRI(NAMESPACE, "column");
		COMMENT_PREFIX = Vocabularies.createIRI(NAMESPACE, "commentPrefix");
		DATATYPE = Vocabularies.createIRI(NAMESPACE, "datatype");
		DECIMAL_CHAR = Vocabularies.createIRI(NAMESPACE, "decimalChar");
		DEFAULT = Vocabularies.createIRI(NAMESPACE, "default");
		DELIMITER = Vocabularies.createIRI(NAMESPACE, "delimiter");
		DESCRIBES = Vocabularies.createIRI(NAMESPACE, "describes");
		HAS_DIALECT = Vocabularies.createIRI(NAMESPACE, "dialect");
		DOUBLE_QUOTE = Vocabularies.createIRI(NAMESPACE, "doubleQuote");
		ENCODING = Vocabularies.createIRI(NAMESPACE, "encoding");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "format");
		GROUP_CHAR = Vocabularies.createIRI(NAMESPACE, "groupChar");
		HEADER = Vocabularies.createIRI(NAMESPACE, "header");
		HEADER_ROW_COUNT = Vocabularies.createIRI(NAMESPACE, "headerRowCount");
		LANG = Vocabularies.createIRI(NAMESPACE, "lang");
		LINE_TERMINATORS = Vocabularies.createIRI(NAMESPACE, "lineTerminators");
		NAME = Vocabularies.createIRI(NAMESPACE, "name");
		NOTE = Vocabularies.createIRI(NAMESPACE, "note");
		NULL = Vocabularies.createIRI(NAMESPACE, "null");
		ORDERED = Vocabularies.createIRI(NAMESPACE, "ordered");
		PROPERTY_URL = Vocabularies.createIRI(NAMESPACE, "propertyUrl");
		QUOTE_CHAR = Vocabularies.createIRI(NAMESPACE, "quoteChar");
		REQUIRED = Vocabularies.createIRI(NAMESPACE, "required");
		HAS_ROW = Vocabularies.createIRI(NAMESPACE, "row");
		HAS_TABLE = Vocabularies.createIRI(NAMESPACE, "table");
		ROWNUM = Vocabularies.createIRI(NAMESPACE, "rownum");
		SEPARATOR = Vocabularies.createIRI(NAMESPACE, "separator");
		SKIP_COLUMNS = Vocabularies.createIRI(NAMESPACE, "skipColumns");
		SKIP_INITIAL_SPACE = Vocabularies.createIRI(NAMESPACE, "skipInitialSpace");
		SKIP_ROWS = Vocabularies.createIRI(NAMESPACE, "skipRows");
		SUPPRESS_OUTPUT = Vocabularies.createIRI(NAMESPACE, "suppressOutput");
		TABLE_SCHEMA = Vocabularies.createIRI(NAMESPACE, "tableSchema");
		TEXT_DIRECTION = Vocabularies.createIRI(NAMESPACE, "textDirection");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		TRIM = Vocabularies.createIRI(NAMESPACE, "trim");
		URL = Vocabularies.createIRI(NAMESPACE, "url");
		VALUE_URL = Vocabularies.createIRI(NAMESPACE, "valueUrl");
		VIRTUAL = Vocabularies.createIRI(NAMESPACE, "virtual");
	}
}
