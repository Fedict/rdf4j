/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * RDF parser for RDFa files.
 * 
 * @author Bart.Hanssens
 */
public class RDFaParser extends AbstractRDFParser implements RDFParser {
	public final static Map<String,String> INITIAL_CONTEXT = RDFaUtil.buildContext();

	private String baseURL = "";

	/**
	 * Creates a new RDFaParser that will use a {@link SimpleValueFactory} to create object for resources,
	 * bNodes and literals.
	 */
	public RDFaParser() {
		super();
	}

	/**
	 * Creates a new RDFaParser that will use the supplied <tt>ValueFactory</tt> to create RDF model objects.
	 * 
	 * @param valueFactory
	 *        A ValueFactory.
	 */
	public RDFaParser(ValueFactory valueFactory) {
		super(valueFactory);
	}
	
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFA;
	}

	@Override
	public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		if (this.rdfHandler != null) {
			this.rdfHandler.startRDF();
		}
		baseURL = baseURI;

		try {
			Document doc = Jsoup.parse(in, null, baseURI);
			NodeTraversor.traverse(new ElementNodeVisitor(), doc);
		} catch (IOException ex) {
			reportFatalError(ex);
		}
		
		if (this.rdfHandler != null) {
			this.rdfHandler.endRDF();
		}
	}

	@Override
	public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		// JSoup Parser.htmlParser().parseInput does not seem to use DataUtil to check document encoding
		try (InputStream is = new InputStream() {
			@Override
			public int read() throws IOException {
				return reader.read();
			}}) {
			parse(is, baseURI);
		}
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		return result;
	}

	/**
	 * Handle a namespace
	 * 
	 * @param prefix
	 * @param ns 
	 */
	protected void handleNS(String prefix, String ns) {
		rdfHandler.handleNamespace(prefix, ns);
	}

	/**
	 * Handle a triple statement
	 * 
	 * @param subj
	 * @param pred
	 * @param obj 
	 */
	protected void handleTriple(Resource subj, IRI pred, Value obj) {
		rdfHandler.handleStatement(valueFactory.createStatement(subj, pred, pred));
	}

	/**
	 * Handle an error
	 * 
	 * @param msg
	 * @throws RDFParseException 
	 */
	protected void error(String msg) throws RDFParseException {
		reportError(msg, null);
	}

	/**
	 * Handle a fatal error
	 * 
	 * @param msg
	 * @throws RDFParseException 
	 */
	protected void fatalError(String msg) throws RDFParseException {
		reportFatalError(msg);
	}

	/**
	 * Class to iterate over XML/XHTML/HTML elements
	 */
	private class ElementNodeVisitor implements NodeVisitor {
		// keep track of language and RDF vocabularies, they may be set on ancestor elements
		private final TreeMap<Integer,String> localLang = new TreeMap<>();
		private final TreeMap<Integer,Map<String,String>> localNS = new TreeMap<>();
		// keep track of RDF subjects, predicates and types/classes, they may be set on ancestor elements
		private final TreeMap<Integer,Resource> pendingSubj = new TreeMap<>();
		private final TreeMap<Integer,IRI> pendingPred = new TreeMap<>();
		private final TreeMap<Integer,IRI[]> pendingTypes = new TreeMap<>();

		/**
		 * Constructor
		 */
		public ElementNodeVisitor() {
			localLang.put(-1, "");
			localNS.put(-1, INITIAL_CONTEXT);

			pendingSubj.put(-1, createURI(baseURL));
			pendingPred.put(-1, null);
		}

		/**
		 * Get language.
		 * The language could be defined on the element itself or on an ancestor element.
		 * 
		 * @param prefix prefix
		 * @return language code
		 */
		private String getLanguage() {
			return localLang.get(localLang.lastKey());
		}

		/**
		 * Get namespace associated with a prefix.
		 * This namespace could be defined on the element itself or on an ancestor element.
		 * 
		 * @param prefix prefix
		 * @return URI of the namespace as string, or null if not found
		 */
		private String getNamespace(String prefix) {
			String ns;
			// a prefix may "hide" a previously set prefix, especially the empty prrefix
			for (Map.Entry<Integer, Map<String,String>> e: localNS.descendingMap().entrySet()) {
				ns = e.getValue().get(prefix);
				if (ns != null) {
					return ns;
				}
			}
			return null;
		}

		/**
		 * Get absolute URL, using baseURL to convert relative URLs.
		 * 
		 * @param str relative URL
		 * @return absolute URL or null
		 */
		private String absoluteURL(String str) {
			if (str == null) {
				return null;
			}
			
			return (str.startsWith("#") || !str.contains(":")) ? baseURL + str : str;
		}

		/**
		 * Get RDF subject.
		 * The subject could be defined on the element itself or on an ancestor element.
		 * 
		 * @return subject IRI/blank node or null
		 */
		private Resource getPendingSubject() {
			return pendingSubj.get(pendingSubj.lastKey());
		}

		/**
		 * Get RDF predicate.
		 * The predicate could be defined on the element itself or on an ancestor element.
		 * 
		 * @return subject IRI or null
		 */
		private IRI getPendingPredicate() {
			return pendingPred.get(pendingPred.lastKey());
		}

		/**
		 * Expand prefixed string
		 * 
		 * @param str
		 * @return fully IRI as a string or null
		 */
		private String expandPrefix(String str) {
			if (str.startsWith("http:") || str.startsWith("https:") || str.startsWith("mailto:")) {
				// already expandend
				return str;
			}
			String[] parts = str.split(":", 2);
			String prefix = (parts.length == 1) ? "" : parts[0].toLowerCase();
			String ns = getNamespace(prefix);
			return (ns != null) ? ns + parts[parts.length-1] : null;
		}

		/**
		 * Get the value of a non empty attribute.
		 * If the attribute is present but its value is empty, an error will be raised.
		 * No error will be raised if the attribute is not present at all.
		 * 
		 * @param el element
		 * @param attr attribute to look for
		 * @return null when attribute is not present or empty
		 */
		private String getNonEmptyAttr(Element el, String attr) {
			String str = el.attr(attr);
			if (str == null) {
				return null;
			}
			if (str.isEmpty()) {
				error("Empty " + attr + " attribute for " + el);
				return null;
			}
			return str;
		}

		/**
		 * Check base href element.
		 * 
		 */
		private void setBase(Element el) {
			String base = el.attr(RDFaUtil.HREF);
			if (base == null) {
				return;
			}
			baseURL = absoluteURL(base);
		}

		/**
		 * Check if the element has an xml:lang or lang attribute (in that order) for declaring the language.
		 * This is only valid for the element itself and its descendants, so it must be removed when 
		 * JSoup is finished with processing the element.
		 * 
		 * @param el element
		 * @param depth depth of the element in the DOM structure
		 */
		private void setLanguage(Element el, int depth) {
			String lang = el.attr(RDFaUtil.XML_LANG);
			if (lang == null) {
				lang = el.attr(RDFaUtil.LANG);
				if (lang != null) {
					localLang.put(depth, lang);
				}
			}
		}

		/**
		 * Check if the element has a vocab attribute for declaring the local vocabulary namespace.
		 * This is only valid for the element itself and its descendants, so it must be removed when 
		 * JSoup is finished with processing the element.
		 * 
		 * @param el element
		 * @param depth depth of the element in the DOM structure
		 */
		private void setVocab(Element el, int depth) {
			String vocab = getNonEmptyAttr(el, RDFaUtil.VOCAB);
			if (vocab == null) {
				return;
			}

			Map<String,String> ctx = localNS.getOrDefault(depth, new HashMap<>());
			ctx.put("", vocab);
			localNS.put(depth, ctx);
			handleNS("", vocab);
		}

		/**
		 * Check if the element has a prefix attribute for declaring local namespaces.
		 * They are only valid for the element itself and its descendants, so they must be removed when 
		 * JSoup is finished with processing the element.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void setPrefixes(Element el, int depth) {
			String prefixes = getNonEmptyAttr(el, RDFaUtil.PREFIX);
			if (prefixes == null) {
				return;
			}

			// look for prefix and namespace, separated with whitespace
			// there can be multiple prefix + namespace pairs in one attribute, also separated with whitespaces
			String[] splits = prefixes.split(" +");
			int len = splits.length;
			if (len % 2 != 0) {
				fatalError("Error processing namespaces " + el.tagName() + " " + prefixes);
				return;
			}

			// Keep track of depth: namespaces are valid for the element itself and descendants,
			// so they must be removed at the end of the element.
			// The prefix can also locally hide/change a prefix previously set by an ancestor element.
			Map<String,String> ctx = localNS.getOrDefault(depth, new HashMap<>());
			for (int i = 0 ; i < len; i += 2) {
				if (splits[i].endsWith(":")) {
					String prefix = splits[i].substring(0, splits[i].length()-1).toLowerCase();
					ctx.put(prefix, splits[i+1]);
					localNS.put(depth, ctx);
					handleNS(prefix, splits[i+1]);
				} else {
					error("Prefix doesn't end with ':' " + el.tagName() + " " + splits[i]);
				}
			}
		}

		/**
		 * Check if the element has a rel attribute for declaring the predicate of a series of triples.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 * @return true if a rel attribute was found
		 */
		private boolean checkRel(Element el, int depth) {
			String rel = getNonEmptyAttr(el, RDFaUtil.REL);
			if (rel == null) {
				return false;
			}
			pendingPred.put(depth, createURI(toAbsolute(rel)));

			return true;
		}

		/**
		 * Get subject, based on various attributes and/or subjects set by ancestor elements.
		 * 
		 * @param about value of about attribute
		 * @param typeof value of typeof attribute
		 * @param attrs number of attributes
		 * @param depth
		 * @return IRI or or null
		 */
		private Resource getNewSubject(String about, String typeof, int attrs, int depth) {
			// per spec, create a blank node if typeof is set as the only attribute
			if (typeof != null && attrs == 1) {
				return createNode(about);
			}
			// maybe the subject was set on an ancestor element
			if (about == null) {
				return getPendingSubject();
			}
			if (about.startsWith("_")) {
				return createNode(about);
			}
			String str = expandPrefix(about);
			if (str == null) {
				error("Could not get current subject");
			}
			return createURI(absoluteURL(str));
		}

		/**
		 * Check if the element has a resource attribute for declaring the subject of a triple.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private boolean checkResource(Element el, int depth) {
			String resource = getNonEmptyAttr(el, RDFaUtil.RESOURCE);
			if (resource == null) {
				return false;
			}

			IRI iri = createURI(toAbsolute(resource));
			// this element or an ancestor may have a rel attribute / triple predicate, making this an object
			if (lastPredLevel != -1 && depth >= lastPredLevel) {
				handleTriple(getSubject(), getPredicate(), iri);
			}

			return true;
		}

		/**
		 * Check if the element has a property attribute for declaring the predicate of a triple.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private IRI getPredicate(String prop, String rel, String dtype, String txt, String href, String src) {
			if (prop == null) {
				return getPendingPredicate();
			}
			String iri = expandPrefix(prop);
			if (iri.isEmpty()) {
				error("Could not find namespace for " + prop);
			}
			return createURI(iri);
		}

		/**
		 * Get RDF object value from an element
		 * 
		 * @param el
		 * @return typed literal, IRI or null
		 */
		private Value getContent(Element el, int depth) {
			// content attribute value takes precedence over text node
			String txt = el.attr(RDFaUtil.CONTENT);
			if (txt == null) {
				txt = el.text();
				if (txt == null) {
					error("No content attribute and no text node " + el);
					return null;
				}
			}
			String type = el.attr(RDFaUtil.DATATYPE);
			if (type != null) {
				type = expandPrefix(type);
				if (type == null) {
					error("Could not expand datatype for " + el);
				}
			}
			IRI datatype = (type != null) ? createURI(type) : XMLSchema.STRING;
			return createLiteral(txt, getLanguage(), datatype);
		}

		/**
		 * Check if the element has a typeOf attribute for declaring the RDF class of the subject.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private boolean checkTypeof(Element el, int depth, String type) {
			if (type == null) {
				return false;
			}

			// typeof without another attribute creates a blank node		
			if (el.attributes().size() == 1) {
				pendingSubj.put(depth, createNode());
			}

			// multiple (whitespace separated) types/classes are allowed
			IRI[] types = Arrays.stream(type.split(" "))
								.map(t -> createURI(expandPrefix(t)))
								.toArray(IRI[]::new);
			pendingTypes.put(depth, types);
			return true;
		}
		
		/**
		 * 
		 * @param values
		 * @return 
		 */
		private boolean allNull(String... values) {
			for(String value: values) {
				if (value != null) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void head(Node node, int i) {
			if (node instanceof Element) {
				Element el = (Element) node;
				int attrs = el.attributes().size();
				if (attrs == 0) {
					return;
				}

				// basic context settings
				if (el.tagName().equals(RDFaUtil.BASE)) {
					setBase(el);
				}				
				setLanguage(el, i);
				setVocab(el, i);
				setPrefixes(el, i);

				String about = getNonEmptyAttr(el, RDFaUtil.ABOUT);
				String prop = getNonEmptyAttr(el, RDFaUtil.PROPERTY);
				String rel = getNonEmptyAttr(el, RDFaUtil.REL);
				String rev = getNonEmptyAttr(el, RDFaUtil.REV);
				String res = getNonEmptyAttr(el, RDFaUtil.RESOURCE);
				String typeof = getNonEmptyAttr(el, RDFaUtil.TYPEOF);

				// check if the element has RDFa attributes that need to be processed
				if (allNull(about, prop, rel, rev, res, typeof)) {
					return;
				}

				Resource subj = null;
				if (rel == null && rev == null) {
					subj = getNewSubject(about, typeof, attrs, i);
				} else {
					subj = getExistingSubject(rel, rev);
					obj = get
				}
				
				
				
				String inlist = el.attr(RDFaUtil.INLIST);
				String datatype =	el.attr(RDFaUtil.DATATYPE);

				String ref = getNonEmptyAttr(el, RDFaUtil.HREF);
				if (ref == null) {
					ref = getNonEmptyAttr(el, RDFaUtil.SRC);
				}

				IRI pred = getPredicate(prop, rel);
				Value obj = getObject();
				IRI[] types;
				
				if (subj != null && pred != null && obj != null) {
					handleTriple(subj, pred, obj);
				}
				for (IRI t: types) {
					handleTriple(subj,RDF.TYPE, t);
				}
			} 
		}

		@Override
		public void tail(Node node, int i) {
			if (node instanceof Element) {
				// remove "out-of-scope" namespaces and languages
				if (i == localNS.lastKey()) {
					localNS.remove(i);
				}
				if (i == localLang.lastKey()) {
					localLang.remove(i);
				}

				
				if (i == pendingSubj.lastKey()) {
					pendingSubj.remove(i);
				}
				if (i == pendingPred.lastKey()) {
					pendingPred.remove(i);
				}
				if (i == pendingTypes.lastKey()) {
					pendingTypes.remove(i);
				}
			}
		}
	}
}
