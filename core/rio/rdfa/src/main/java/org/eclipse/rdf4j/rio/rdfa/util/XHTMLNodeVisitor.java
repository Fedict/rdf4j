/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.rio.rdfa.RDFaParser;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

/**
 * Class to iterate over XML/XHTML/HTML elements
 *
 * See <a href="https://www.w3.org/TR/rdfa-core/#s_model">RDFa Processing Model</a>
 *
 * @author Bart Hanssens
 * @see https://www.w3.org/TR/rdfa-core/#s_model
 */
@InternalUseOnly
public class XHTMLNodeVisitor implements NodeVisitor {
	public final static Map<String, String> INITIAL_CONTEXT = RDFaUtil.buildContext();

	private final RDFaParser parser;
	private final EvaluationContext initialContext;
	private final Map<Integer, EvaluationContext> localContexts = new TreeMap<>();
	
	/**
	 * Constructor
	 * 
	 * @param parser
	 * @param baseURI
	 */
	public XHTMLNodeVisitor(RDFaParser parser, String baseURI) {
		this.parser = parser;

		initialContext = new EvaluationContext();
		initialContext.setBase(baseURI);
		initialContext.setIriMappings(Collections.EMPTY_MAP);
		initialContext.setTermMappings(Collections.EMPTY_MAP);
		initialContext.setDefaultVocabulary(null);
		
	}

	/**
	 * Get language. The language could be defined on the element itself or on an ancestor element.
	 *
	 * @param prefix prefix
	 * @return language code
	 */
/*	private String getLanguage() {
		return localLang.get(localLang.lastKey());
	}
*/
	/**
	 * Get namespace associated with a prefix. This namespace could be defined on the element itself or on an ancestor
	 * element.
	 *
	 * @param prefix prefix
	 * @return URI of the namespace as string, or null if not found
	 */
/*	private String getNamespace(String prefix) {
		String ns;
		// a prefix may "hide" a previously set prefix, especially the empty prrefix
		for (Map.Entry<Integer, Map<String, String>> e : localNS.descendingMap().entrySet()) {
			ns = e.getValue().get(prefix);
			if (ns != null) {
				return ns;
			}
		}
		return null;
	}
*/
	/**
	 * Get absolute URL, using baseURL to convert relative URLs.
	 *
	 * @param str relative URL
	 * @return absolute URL or null
	 */
/*	private String absoluteURL(String str) {
		return (str == null || str.startsWith("_:")) ? str : parser.baseURL + str;
	}
*/
	/**
	 * Get RDF subject. The subject could be defined on the element itself or on an ancestor element.
	 *
	 * @return subject IRI/blank node or null
	 */
/*	private Resource getParentSubj() {
		return parentSubj.get(parentSubj.lastKey());
	}
*/
	/**
	 * Get RDF predicate. The predicate could be defined on the element itself or on an ancestor element.
	 *
	 * @return subject IRI or null
	 */
/*	private IRI getParentPred() {
		return parentPred.get(parentPred.lastKey());
	}
*/
	/**
	 * Get RDF object. The object could be defined on the element itself or on an ancestor element.
	 *
	 * @return object resource or null
	 */
/*	private Resource getParentObj() {
		return parentObj.get(parentObj.lastKey());
	}
*/
	/**
	 * Expand prefixed string
	 *
	 * @param str
	 * @return fully IRI as a string or null
	 */
/*	private String expandPrefix(String str) {
		if (str.startsWith("_:")) {
			// blank node
			return str;
		}
		if (str.startsWith("http:") || str.startsWith("https:") || str.startsWith("mailto:")) {
			// already expandend
			return str;
		}
		// Safe CURIE
		if (str.startsWith("[") && str.endsWith("]")) {
			str = str.substring(1, str.length() - 1);
		}

		String[] parts = str.split(":", 2);
		String prefix = (parts.length == 1) ? "" : parts[0].toLowerCase();
		String ns = getNamespace(prefix);
		return (ns != null) ? ns + parts[parts.length - 1] : null;
	}
*/
	/**
	 * Get the value of a non empty attribute. If the attribute is present but its value is empty, an error will be
	 * raised. No error will be raised if the attribute is not present at all.
	 *
	 * @param el   element
	 * @param attr attribute to look for
	 * @return null when attribute is not present or empty
	 */
	private String getNonEmptyAttr(Element el, String attr) {
		String str = el.attr(attr);
		if (str == null) {
			return null;
		}
		if (str.isEmpty()) {
			parser.error("Empty " + attr + " attribute for " + el);
			return null;
		}
		return str;
	}

	/**
	 * Check base href element.
	 *
	 */
/*	private void setBase(Element el) {
		String base = el.attr(RDFaUtil.HREF);
		if (base == null) {
			return;
		}
		parser.baseURL = absoluteURL(base);
	}
*/
	/**
	 * Check if the element has an xml:lang or lang attribute (in that order) for declaring the language. 
	 * 
	 * This is only
	 * valid for the element itself and its descendants, so it must be removed when JSoup is finished with processing
	 * the element.
	 *
	 * @param el element
	 * @return language tag or null
	 */
	private String getLanguage(Element el) {
		String lang = el.attr(RDFaUtil.XML_LANG);
		return (lang != null) ? lang : el.attr(RDFaUtil.LANG);
	}

	/**
	 * Check if the element has a vocab attribute for declaring the local vocabulary namespace. 
	 * 
	 * This is only valid for
	 * the element itself and its descendants, so it must be removed when JSoup is finished with processing the element.
	 *
	 * @param el element
	 * @return default vocabulary, can be empty or null
	 */
	private String getVocab(Element el) {
		return getNonEmptyAttr(el, RDFaUtil.VOCAB);
	}

	/**
	 * Check if the element has a prefix attribute for declaring local namespaces. 
	 * 
	 * They are only valid for the element
	 * itself and its descendants, so they must be removed when JSoup is finished with processing the element.
	 *
	 * @param el element
	 * @return map of prefixes or null
	 */
	private Map<String,String> getPrefixes(Element el) {
		String prefixes = getNonEmptyAttr(el, RDFaUtil.PREFIX);
		if (prefixes == null) {
			return null;
		}

		// look for prefix and namespace, separated with whitespace
		// there can be multiple prefix + namespace pairs in one attribute, also separated with whitespaces
		String[] splits = prefixes.split(" +");
		int len = splits.length;
		if (len % 2 != 0) {
			parser.fatalError("Error processing namespaces " + el.tagName() + " " + prefixes);
			return null;
		}

		// Keep track of depth: namespaces are valid for the element itself and descendants,
		// so they must be removed at the end of the element.
		// The prefix can also locally hide/change a prefix previously set by an ancestor element.
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < len; i += 2) {
			if (splits[i].endsWith(":")) {
				String prefix = splits[i].substring(0, splits[i].length() - 1).toLowerCase();
				map.put(prefix, splits[i + 1]);
				parser.handleNS(prefix, splits[i + 1]);
			} else {
				parser.error("Prefix doesn't end with ':' " + el.tagName() + " " + splits[i]);
			}
		}
		return map;
	}

	/**
	 * Get subject, based on various attributes and/or subjects set by ancestor elements.
	 *
	 * @param about value of about attribute
	 * @param depth
	 * @return Resource or null
	 */
/*	private Resource getNewSubj(String about, int depth) {
		if (about != null) {
			return parser.createIRI(absoluteURL(expandPrefix(about)));
		}
		return (depth == 0) ? parser.createIRI(parser.baseURL) : getParentObj();
	}

	private Resource getNewSubj(String about, String res, String href, String src, String typeof, int depth) {
		String str = RDFaUtil.firstNonNull(about, res, href, src);
		if (str != null) {
			return parser.createIRI(absoluteURL(expandPrefix(str)));
		}
		if (depth == 0) {
			return parser.createIRI(parser.baseURL);
		}
		return (typeof != null) ? parser.createBlank() : getParentObj();
	}

	private Resource getTypeSubj(String about, String res, String href, String src, String typeof, int depth) {
		if (about != null) {
			return parser.createIRI(absoluteURL(expandPrefix(about)));
		}
		if (depth == 0) {
			return parser.createIRI(parser.baseURL);
		}
		String str = RDFaUtil.firstNonNull(res, href, src);
		return (str != null) ? parser.createIRI(absoluteURL(expandPrefix(str))) : parser.createBlank();
	}

	private Resource getNewObj(String about, String res, String href, String src, String typeof) {
		String str = RDFaUtil.firstNonNull(about, res, href, src);
		if (str != null) {
			return parser.createIRI(absoluteURL(expandPrefix(str)));
		}
		return (typeof != null) ? parser.createBlank() : null;
	}
*/
	/**
	 * Check if the element has a property attribute for declaring the predicate of a triple.
	 *
	 * @param el    element
	 * @param depth depth in DOM structure
	 */
/*	private IRI getPredicate(String prop, String rel, String dtype, String txt, String href, String src) {
		if (prop == null) {
			return getParentPred();
		}
		String iri = expandPrefix(prop);
		if (iri.isEmpty()) {
			parser.error("Could not find namespace for " + prop);
		}
		return parser.createIRI(iri);
	}
*/
	/**
	 * Get RDF object value from an element
	 *
	 * @param el
	 * @return typed literal, IRI or null
	 */
/*	private Value getContent(Element el, int depth) {
		// content attribute value takes precedence over text node
		String txt = el.attr(RDFaUtil.CONTENT);
		if (txt == null) {
			txt = el.text();
			if (txt == null) {
				parser.error("No content attribute and no text node " + el);
				return null;
			}
		}
		String type = el.attr(RDFaUtil.DATATYPE);
		if (type != null) {
			type = expandPrefix(type);
			if (type == null) {
				parser.error("Could not expand datatype for " + el);
			}
		}
		IRI datatype = (type != null) ? createURI(type) : XSD.STRING;
		return createLiteral(txt, getLanguage(), datatype);
	}
*/
	/**
	 * Convert a string containing one or more (space-separated) properties to an array of full IRIs
	 *
	 * @param str string
	 * @return array of IRIs
	 */
/*	private IRI[] toIRIArray(String str) {
		return Arrays.stream(str.split(" "))
				.map(t -> parser.createIRI(expandPrefix(t)))
				.toArray(IRI[]::new);
	}
*/
	@Override
	public void head(Node node, int i) {
		if (!(node instanceof Element)) {
			return;
		}

		// See processing rules 7.5 https://www.w3.org/TR/rdfa-core/#s_sequence
		Element el = (Element) node;

		int attrs = el.attributes().size();
		if (attrs == 0) {
			return;
		}

		EvaluationContext localContext = new EvaluationContext();
		localContexts.put(i, localContext);
		localContext.setSkipElement(false);
		
		// basic context settings
		// 7.5.2 - 7.5.4
		localContext.setDefaultVocabulary(getVocab(el));
		localContext.setIriMappings(getPrefixes(el));
		localContext.setLanguage(getLanguage(el));

		// check "special" attributes
		String about = el.attr(RDFaUtil.ABOUT);
		String href = el.attr(RDFaUtil.HREF);
		String inlist = el.attr(RDFaUtil.INLIST);
		String prop = el.attr(RDFaUtil.PROPERTY);
		String rel = el.attr(RDFaUtil.REL);
		String res = el.attr(RDFaUtil.RESOURCE);
		String rev = el.attr(RDFaUtil.REV);
		String src = el.attr(RDFaUtil.SRC);
		String typeof = el.attr(RDFaUtil.TYPEOF);

		// check if the element has an RDFa attribute that starts the processing
		if (RDFaUtil.firstNonNull(about, href, inlist, prop, rel, rev, res, src, typeof) != null) {
			return;
		}

		// used for (typed) literals
		String content = getNonEmptyAttr(el, RDFaUtil.CONTENT);
		String datatype = getNonEmptyAttr(el, RDFaUtil.DATATYPE);

		if (rel == null && rev == null) {
			// 7.5.5
			if (prop != null && content == null && datatype == null) {
				newSubject = getNewSubj(about, i);
				if (typeof != null) {
					typedRes = getTypeSubj(about, res, href, src, typeof, i);
					if (about == null) {
						currObjRes = typedRes;
					}
				}
			} else {
				newSubject = getNewSubj(about, res, href, src, typeof, i);
				if (typeof != null) {
					typedRes = newSubject;
				}
			}
		} else {
			// 7.5.6
			newSubject = getNewSubj(about, i);
			if (typeof != null) {
				typedRes = newSubject;
			}
			currObjRes = getNewObj(about, res, href, src, typeof);
			if (typeof != null && about == null) {
				typedRes = currObjRes;
			}
		}

		// 7.5.7
		if (typedRes != null && typeof != null) {
			IRI[] types = toIRIArray(typeof);
			for (IRI t : types) {
				parser.handleTriple(newSubject, RDF.TYPE, t);
			}
		}

		// 7.5.8
		if (newSubject != null && newSubject != getParentSubj()) {
			// list = new ArrayList<>();
		}

		if (currObjRes != null) {
			// 7.5.9
			if (inlist != null && rel != null) {
				// TODO
			}
			if (inlist == null && rel != null) {
				IRI[] preds = toIRIArray(rel);
				for (IRI pred : preds) {
					parser.handleTriple(newSubject, pred, currObjRes);
				}
			}
			if (rev != null) {
				IRI[] preds = toIRIArray(rel);
				for (IRI pred : preds) {
					parser.handleTriple(currObjRes, pred, newSubject);
				}
			}
		} else {
			// 7.5.10
		}
	}

	@Override
	public void tail(Node node, int i) {
		if (node instanceof Element) {
			// remove "out-of-scope" namespaces and languages etc
			localContexts.remove(i);
		}
	}
}
