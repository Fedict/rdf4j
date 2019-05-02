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

	private class ElementNodeVisitor implements NodeVisitor {
		// keep track of vocabularies, subjects and language
		private final TreeMap<Integer,Map<String,String>> localNS = new TreeMap<>();
		private int lastNSLevel = -1;
		private final TreeMap<Integer,Resource> localSubj = new TreeMap<>();
		private int lastSubjLevel = -1;
		private final TreeMap<Integer,Resource> localPred= new TreeMap<>();
		private int lastPredLevel = -1;
		private final TreeMap<Integer,String> localLang = new TreeMap<>();
		private int lastLangLevel = -1;

		/**
		 * Constructor
		 */
		public ElementNodeVisitor() {
			localNS.put(lastNSLevel, INITIAL_CONTEXT);
			localSubj.put(lastSubjLevel, createNode());
			localLang.put(lastLangLevel, "");
			// localPred remains empty
		}

		/**
		 * Get absolute URI, using baseURL to convert relative URLs and local names.
		 * 
		 * @param str string
		 * @return absolute URL
		 */
		private String toAbsolute(String str) {
			return (str.startsWith("#") || !str.contains(":")) ? baseURL + str : str;
		}

		/**
		 * Get subject.
		 * The subject could be defined on the element itself or on an ancestor element.
		 * 
		 * @return subject IRI/blank node or null
		 */
		private Resource getSubject() {
			return localSubj.get(localSubj.lastKey());
		}

		/**
		 * Get predicate.
		 * The predicate could be defined on the element itself or on an ancestor element.
		 * 
		 * @return subject IRI or null
		 */
		private Resource getPredicate() {
			return localPred.get(localPred.lastKey());
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
			for (Map.Entry<Integer, Map<String,String>> e: localNS.descendingMap().entrySet()) {
				ns = e.getValue().get(prefix);
				if (ns != null) {
					return ns;
				}
			}
			return null;
		}

		/**
		 * Expand prefixed string
		 * 
		 * @param str
		 * @return fully IRI as a string
		 */
		private String expandPrefix(String str) {
			String[] parts = str.split(":", 2);
			String prefix = (parts.length == 1) ? "" : parts[0];
			String ns = getNamespace(prefix);
			return (ns != null) ? ns + parts[parts.length-1] : null;
		}

		/**
		 * Check if the element has an xml:lang or lang attribute (in that order) for declaring the language.
		 * This is only valid for the element itself and its descendants, so it must be removed when 
		 * JSoup is finished with processing the element.
		 * 
		 * @param el element
		 * @param depth depth of the element in the DOM structure
		 */
		private void checkLanguage(Element el, int depth) {
			String lang = el.attr(RDFaUtil.XML_LANG);
			if (lang == null) {
				lang = el.attr(RDFaUtil.LANG);
				if (lang != null) {
					localLang.put(depth, lang);
					lastLangLevel = depth;
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
		private void checkVocab(Element el, int depth) {
			String vocab = el.attr(RDFaUtil.VOCAB);
			if (vocab == null || vocab.isEmpty()) {
				return;
			}

			Map<String, String> ctx = new HashMap<>();
			ctx.put("", vocab);
			handleNS("", vocab);
			localNS.put(depth, ctx);
			lastNSLevel = depth;
		}

		/**
		 * Check if the element has a prefix attribute for declaring local namespaces.
		 * They are only valid for the element itself and its descendants, so they must be removed when 
		 * JSoup is finished with processing the element.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void checkPrefixes(Element el, int depth) {
			String prefixes = el.attr(RDFaUtil.PREFIX);
			if (prefixes == null || prefixes.isEmpty()) {
				return;
			}

			// look for prefix and namespace, separated with whitespace
			// there can be multiple prefix + namespace pairs in one attribute, also separated with whitespaces
			String[] splits = prefixes.split(" +");
			int len = splits.length;
			if (len % 2 != 0) {
				fatalError("Error processing namespaces " + el.tagName() + " " + prefixes);
			}

			// Keep track of depth: namespaces are valid for the element itself and descendants,
			// so they must be removed at the end of the element.
			// The prefix can also locally hide/change a prefix previously set by an ancestor element.
			Map<String,String> ctx = localNS.getOrDefault(depth, new HashMap<>());
			for (int i = 0 ; i < len; i += 2) {
				if (splits[i].endsWith(":")) {
					String prefix = splits[i].substring(0, splits[i].length()-1);
					ctx.put(prefix, splits[i+1]);
					handleNS(prefix, splits[i+1]);
				} else {
					error("Prefix doesn't end with ':' " + el.tagName() + " " + splits[i]);
				}
				localNS.put(depth, ctx);
				lastNSLevel = depth;
			}
		}

		/**
		 * Check if the element has a rel attribute for declaring the predicate of a series of triples.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void checkRel(Element el, int depth) {
			String rel = el.attr(RDFaUtil.REL);
			if (rel == null) {
				return;
			}
			if (rel.isEmpty()) {
				error("Empty rel attribute for " + el);
			}
			localPred.put(depth, createURI(toAbsolute(rel)));
			lastPredLevel = depth;
		}

		/**
		 * Check if the element has a resource attribute for declaring the subject of a triple.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void checkResource(Element el, int depth) {
			String resource = el.attr(RDFaUtil.RESOURCE);
			if (resource == null) {
				return;
			}
			if (resource.isEmpty()) {
				error("Empty resource attribute for " + el);
			}
			localSubj.put(depth, createURI(toAbsolute(resource)));
			lastSubjLevel = depth;
		}

		/**
		 * Check if the element has a property attribute for declaring the predicate of a triple.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void checkProperty(Element el, int depth) {
			String prop = el.attr(RDFaUtil.PROPERTY);
			if (prop == null || prop.isEmpty()) {
				return;
			}
			String iri = expandPrefix(prop);
			if (iri.isEmpty()) {
				error("Could not find namespace for " + el + " " + prop);
			}
			IRI pred = createURI(iri);

			Value val;
			// href or src attribute value takes precedence over text content
			String url = el.attr(RDFaUtil.HREF);
			if (url == null) {
				url = el.attr(RDFaUtil.SRC);
			}
			
			if (url == null) {
				// content attribute value takes precedence over text node
				String txt = el.attr(RDFaUtil.CONTENT);
				if (txt == null) {
					txt = el.text();
					if (txt == null) {
						error("No href/src attribute, no content attribute and no text node " + el);
						txt = "";
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
				val = createLiteral(txt, getLanguage(), datatype);
			} else {
				val = createURI(url);
			}
			handleTriple(getSubject(), pred, val);
		}

		/* Check if the element has a typeOf attribute for declaring the RDF class of the subject.
		 * 
		 * @param el element
		 * @param depth depth in DOM structure
		 */
		private void checkTypeof(Element el, int depth) {
			String type = el.attr(RDFaUtil.TYPEOF);
			if (type == null || type.isEmpty()) {
				return;
			}
			IRI obj = createURI(type);

			// typeof without resource implictly creates a blank node subject
			if (lastSubjLevel != depth) {
				Resource blank = createNode();
				localSubj.put(depth, blank);
				lastSubjLevel = depth;
			}
			handleTriple(getSubject(), RDF.TYPE, obj);
		}

		@Override
		public void head(Node node, int i) {
			if (node instanceof Element) {
				Element el = (Element) node;

				checkLanguage(el, i);
				
				checkVocab(el, i);
				checkPrefixes(el, i);

				checkRel(el, i);

				checkResource(el, i);
				checkProperty(el, i);
				checkTypeof(el, i);
			} 
		}

		@Override
		public void tail(Node node, int i) {
			if (node instanceof Element) {
				// remove out-of-scope namespaces, subject and language
				if (i == lastNSLevel) {
					localNS.remove(i);
					lastNSLevel = localNS.lastKey();
				}
				if (i == lastSubjLevel) {
					localSubj.remove(i);
					lastSubjLevel = localSubj.lastKey();
				}
				if (i == lastPredLevel) {
					localPred.remove(i);
					lastPredLevel = localPred.lastKey();
				}
				if (i == lastLangLevel) {
					localLang.remove(i);
					lastLangLevel = localLang.lastKey();
				}
			}
		}
	}
}
