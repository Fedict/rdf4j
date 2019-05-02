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
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

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

	protected void handleTriple(Resource subj, IRI pred, Value obj) {
		rdfHandler.handleStatement(valueFactory.createStatement(subj, pred, pred));
	}

	protected void error(String msg) throws RDFParseException {
		reportError(msg, null);
	}

	protected void fatalError(String msg) throws RDFParseException {
		reportFatalError(msg);
	}

	private class ElementNodeVisitor implements NodeVisitor {
		private final static String HREF = "href";
		private final static String SRC = "src";
		private final static String LANG = "lang";
		private final static String XML_LANG = "xml:lang";
		// RDFa-Lite subset
		private final static String PREFIX = "prefix";
		private final static String PROPERTY = "property";
		private final static String RESOURCE = "resource";
		private final static String TYPEOF = "typeof";
		private final static String VOCAB = "vocab";
		// Other RDFa "full"
		private final static String ABOUT = "about";
		private final static String CONTENT = "content";
		private final static String REL = "rel";

		// keep track of vocabularies and subjects
		private final TreeMap<Integer,Map<String,String>> localNS = new TreeMap<>();
		private final TreeMap<Integer,Resource> localSubject = new TreeMap<>();
		private final TreeMap<Integer,String> localLang = new TreeMap<>();
	
		/**
		 * Constructor
		 */
		public ElementNodeVisitor() {
			localNS.put(0, INITIAL_CONTEXT);
			localSubject.put(0, createNode());
			localLang.put(0, "");
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
		 * @return subject IRI or blank node
		 */
		private Resource getSubject() {
			return localSubject.get(localSubject.lastKey());
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
		 * @return URI of the namespace as string
		 */
		private String getNamespace(String prefix) {
			String ns = "";
			for (Map.Entry<Integer, Map<String,String>> e: localNS.descendingMap().entrySet()) {
				ns = e.getValue().getOrDefault(prefix, "");
				if (!ns.isEmpty()) {
					return ns;
				}
			}
			return ns;
		}

		private void checkLanguage(Element el, int depth) {
			String lang = el.attr(XML_LANG);
			if (lang == null) {
				lang = el.attr(LANG);
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
		private void checkVocab(Element el, int depth) {
			String vocab = el.attr(VOCAB);
			if (vocab == null || vocab.isEmpty()) {
				return;
			}

			Map<String, String> ctx = new HashMap<>();
			ctx.put("", vocab);
			localNS.put(depth, ctx);
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
			String prefixes = el.attr(PREFIX);
			if (prefixes == null || prefixes.isEmpty()) {
				return;
			}

			// look for prefix and namespace, separated with whitespace
			// there can be multiple prefix + namespace pairs in one attribute, also separated with whitespaces
			String[] splits = prefixes.split(" +");
			int len = splits.length;
			if (len < 2) {
				error("Empty prefix or namespace " + el.tagName() + " " + prefixes);
				return;
			}
			if (len % 2 != 0) {
				error("Empty prefix or namespace " + el.tagName() + " " + prefixes);
				len--;
			}
			// Keep track of depth: namespaces are valid for the element itself and descendants,
			// so they must be removed at the end of the element.
			// The prefix can also locally hide/change a prefix previously set by an ancestor element.
			Map<String,String> ctx = localNS.getOrDefault(depth, new HashMap<>());
			for (int i = 0 ; i < len; i += 2) {
				if (splits[i].endsWith(":")) {
					String prefix = splits[i].substring(0, splits[i].length()-1);
					ctx.put(prefix, splits[i+1]);
				} else {
					error("Prefix doesn't end with ':' " + el.tagName() + " " + splits[i]);
				}
				localNS.put(depth, ctx);
			}
		}

		private void checkResource(Element el, int depth) {
			String resource = el.attr(RESOURCE);
			if (resource == null || resource.isEmpty()) {
				return;
			}
			localSubject.put(depth, createURI(toAbsolute(resource)));
		}

		/**
		 * Check if the element has a property attribute for declaring the predicate of a triple.
		 * 
		 * @param el element
		 * @param depth 
		 */
		private void checkProperty(Element el, int depth) {
			String prop = el.attr(PROPERTY);
			if (prop == null || prop.isEmpty()) {
				return;
			}
			
			String[] splits = prop.split(":", 2);
			String prefix = (splits.length == 1) ? "" : splits[0];
			String ns = getNamespace(prefix);
			if (ns.isEmpty()) {
				error("Could not find namespace for " + el + " " + prefix);
			}
			IRI pred = createURI(ns + splits[splits.length-1]);

			Value val;
			// href or src attribute value takes precedence over text content
			String url = el.attr(HREF);
			if (url == null) {
				url = el.attr(SRC);
			}
			if (url == null) {
				String txt = el.text();
				if (txt == null) {
					error("No href/src attribute and no content " + el);
					txt = "";
				}
				val = createLiteral(txt, getLanguage(), XMLSchema.STRING);
			} else {
				val = createURI(url);
			}
			handleTriple(getSubject(), pred, val);
		}

		private void checkTypeof(Element el, int depth) {
			String type = el.attr(TYPEOF);
			if (type == null || type.isEmpty()) {
				return;
			}
			IRI obj = createURI(type);
			handleTriple(getSubject(), RDF.TYPE, obj);
		}

		@Override
		public void head(Node node, int i) {
			if (node instanceof Element) {
				Element el = (Element) node;

				checkLanguage(el, i);
				
				checkVocab(el, i);
				checkPrefixes(el, i);

				checkResource(el, i);
				checkProperty(el, i);
				checkTypeof(el, i);
			} 
		}

		@Override
		public void tail(Node node, int i) {
			if (node instanceof Element) {
				// remove out-of-scope namespaces, subject and language
				localNS.remove(i);
				localSubject.remove(i);
				localLang.remove(i);
			}
		}
	}
}
