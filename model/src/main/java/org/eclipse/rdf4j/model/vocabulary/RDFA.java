/**
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


/**
 * Constants for HTML+RDFa 1.1 - Second Edition.
 *
 * @see <a href="https://www.w3.org/TR/2015/REC-html-rdfa-20150317/">HTML+RDFa 1.1 - Second Edition</a>
 *
 * @author Bart Hanssens
 */
public class RDFA {
	/**
	 * The RDFa namespace: http://www.w3.org/ns/rdfa#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/rdfa#";

	/**
	 * Recommended prefix for the namespace: "rdfa"
	 */
	public static final String PREFIX = "rdfa";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes
	/** rdfa:DocumentError */
	public static final IRI DOCUMENT_ERROR;

	/** rdfa:Error */
	public static final IRI ERROR;

	/** rdfa:Info */
	public static final IRI INFO;

	/** rdfa:PGClass */
	public static final IRI PGCLASS;

	/** rdfa:Pattern */
	public static final IRI PATTERN;

	/** rdfa:PrefixMapping */
	public static final IRI PREFIX_MAPPING;

	/** rdfa:PrefixOrTermMapping */
	public static final IRI PREFIX_OR_TERM_MAPPING;

	/** rdfa:PrefixRedefinition */
	public static final IRI PREFIX_REDEFINITION;

	/** rdfa:TermMapping */
	public static final IRI TERM_MAPPING;

	/** rdfa:UnresolvedCURIE */
	public static final IRI UNRESOLVED_CURIE;

	/** rdfa:UnresolvedTerm */
	public static final IRI UNRESOLVED_TERM;

	/** rdfa:VocabReferenceError */
	public static final IRI VOCAB_REFERENCE_ERROR;

	/** rdfa:Warning */
	public static final IRI WARNING;


	// Properties
	/** rdfa:context */
	public static final IRI CONTEXT;

	/** rdfa:copy */
	public static final IRI COPY;

	/** rdfa:prefix */
	public static final IRI PREFIX_PROP;

	/** rdfa:term */
	public static final IRI TERM;

	/** rdfa:uri */
	public static final IRI URI;

	/** rdfa:usesVocabulary */
	public static final IRI USES_VOCABULARY;

	/** rdfa:vocabulary */
	public static final IRI VOCABULARY;


	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		DOCUMENT_ERROR = factory.createIRI(NAMESPACE, "DocumentError");
		ERROR = factory.createIRI(NAMESPACE, "Error");
		INFO = factory.createIRI(NAMESPACE, "Info");
		PGCLASS = factory.createIRI(NAMESPACE, "PGClass");
		PATTERN = factory.createIRI(NAMESPACE, "Pattern");
		PREFIX_MAPPING = factory.createIRI(NAMESPACE, "PrefixMapping");
		PREFIX_OR_TERM_MAPPING = factory.createIRI(NAMESPACE, "PrefixOrTermMapping");
		PREFIX_REDEFINITION = factory.createIRI(NAMESPACE, "PrefixRedefinition");
		TERM_MAPPING = factory.createIRI(NAMESPACE, "TermMapping");
		UNRESOLVED_CURIE = factory.createIRI(NAMESPACE, "UnresolvedCURIE");
		UNRESOLVED_TERM = factory.createIRI(NAMESPACE, "UnresolvedTerm");
		VOCAB_REFERENCE_ERROR = factory.createIRI(NAMESPACE, "VocabReferenceError");
		WARNING = factory.createIRI(NAMESPACE, "Warning");

		CONTEXT = factory.createIRI(NAMESPACE, "context");
		COPY = factory.createIRI(NAMESPACE, "copy");
		PREFIX_PROP = factory.createIRI(NAMESPACE, "prefix");
		TERM = factory.createIRI(NAMESPACE, "term");
		URI = factory.createIRI(NAMESPACE, "uri");
		USES_VOCABULARY = factory.createIRI(NAMESPACE, "usesVocabulary");
		VOCABULARY = factory.createIRI(NAMESPACE, "vocabulary");
	}
}