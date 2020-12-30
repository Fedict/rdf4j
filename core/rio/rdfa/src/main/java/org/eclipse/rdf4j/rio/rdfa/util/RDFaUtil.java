/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;


import java.util.Map;
import java.util.TreeMap;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;

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

	public enum Direction {
		REVERSE,
		NONE,
		FORWARD;
	}
	
	/**
	 * Return first non null value
	 * 
	 * @param values list of values
	 * @return string value
	 */
	public static String firstNonNull(String... values) {
		for (String value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Get namespace associated with a prefix. 
	 * 
	 * This namespace could be defined on the element itself or on an ancestor element.
	 *
	 * @param prefix prefix
	 * @return URI of the namespace as string, or null if not found
	 */
	public static String getNamespace(String prefix, EvaluationContext initial, TreeMap<Integer, EvaluationContext> locals) {
		String ns;
		// a prefix may "hide" a previously set prefix, especially the empty prrefix
		for (Map.Entry<Integer, EvaluationContext> e : locals.descendingMap().entrySet()) {
			ns = e.getValue().getIriMappings().get(prefix);
			if (ns != null) {
				return ns;
			}
		}
		return initial.getIriMappings().get(prefix);
	}

	/**
	 * Expand string to IRI
	 * 
	 * @param str
	 * @param initialContext
	 * @param localContexts
	 * @return 
	 */
	public static String expand(String str, EvaluationContext initialContext, TreeMap<Integer, EvaluationContext> localContexts) {
		String s = CURIEUtil.toString(str);
		String ns = getNamespace(s, initialContext, localContexts);
		return ns + s;
	}
}
