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
	/** csvw:basee */
	public static final IRI BASE;

	/** csvw:datatype */
	public static final IRI DATATYPE;

	/** csvw:default */
	public static final IRI DEFAULT;

	/** csvw:lang */
	public static final IRI LANG;

	/** csvw:propertyUrl */
	public static final IRI PROPERTY_URL;

	/** csvw:tableSchema */
	public static final IRI TABLE_SCHEMA;

	/** csvw:url */
	public static final IRI URL;

	/** csvw:valueUrl */
	public static final IRI VALUE_URL;

	static {
		BASE = Vocabularies.createIRI(NAMESPACE, "base");
		DATATYPE = Vocabularies.createIRI(NAMESPACE, "datatype");
		DEFAULT = Vocabularies.createIRI(NAMESPACE, "default");
		LANG = Vocabularies.createIRI(NAMESPACE, "lang");
		PROPERTY_URL = Vocabularies.createIRI(NAMESPACE, "propertyUrl");
		TABLE_SCHEMA = Vocabularies.createIRI(NAMESPACE, "tableSchema");
		URL = Vocabularies.createIRI(NAMESPACE, "url");
		VALUE_URL = Vocabularies.createIRI(NAMESPACE, "valueUrl");
	}
}
