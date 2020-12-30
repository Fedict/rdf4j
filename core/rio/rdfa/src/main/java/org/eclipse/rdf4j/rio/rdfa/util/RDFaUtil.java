/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;


import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 *
 * @author Bart.Hanssens
 */
@InternalUseOnly
class RDFaUtil {
	// RDFa-Lite subset
	public final static String PREFIX = "prefix";
	public final static String PROPERTY = "property";
	public final static String RESOURCE = "resource";
	public final static String TYPEOF = "typeof";
	public final static String VOCAB = "vocab";
	// Other RDFa "full"
	public final static String ABOUT = "about";
	public final static String BASE = "base";
	public final static String CONTENT = "content";
	public final static String DATATYPE = "datatype";
	public final static String DATETIME = "datetime";
	public final static String HREF = "href";
	public final static String INLIST = "inlist";
	public final static String LANG = "lang";
	public final static String REL = "rel";
	public final static String REV = "rev";
	public final static String SRC = "src";
	public final static String VERSION = "version";
	public final static String XML_BASE = "xml:base";
	public final static String XML_LANG = "xml:lang";
	public final static String XMLNS = "xmlns";

	public static String firstNonNull(String... values) {
		for (String value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	public enum Direction {
		REVERSE,
		NONE,
		FORWARD;
	}
}
