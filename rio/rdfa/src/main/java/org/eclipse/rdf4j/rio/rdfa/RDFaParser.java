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

	protected IRI createIRI(String str) {
		try {
			return valueFactory.createIRI(str);
		} catch (IllegalArgumentException iae) {
			reportError(iae, BasicParserSettings.VERIFY_URI_SYNTAX);
		}
		return null;
	}

	protected void createTriple(Resource subj, IRI pred, Value obj) {
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
		private final TreeMap<Integer,Map<String,String>> localNS;
		private final TreeMap<Integer,Resource> localSubject;
	
		protected ElementNodeVisitor() {
			localNS = new TreeMap<>();
			localNS.put(0, INITIAL_CONTEXT);

			localSubject = new TreeMap<>();
		}

		protected String geFullURI(String str) {
			return (str.startsWith("#") || !str.contains(":")) ? baseURL + str : str;
		}

		private String getNamespace(String str) {
			String ns = "";
			for (Map.Entry<Integer, Map<String,String>> e: localNS.descendingMap().entrySet()) {
				ns = e.getValue().getOrDefault(str, "");
				if (!ns.isEmpty()) {
					return ns;
				}
			}
			return ns;
		}
	
		/**
		 * Check if the element has a vocab attribute for declaring local namespace.
		 * 
		 * @param el
		 * @param depth 
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
		 * 
		 * @param el
		 * @param depth 
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
			// keep track of depth: namespaces are also valid for child elements,
			// but should be removed at the end of the element
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
			IRI subj = createIRI(getFullURI(resource));
		}

		/**
		 * Check if the element has a property attribute for declaring predicate.
		 * 
		 * @param el
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
			IRI pred = createIRI(ns + splits[splits.length-1]);

			// href or src attribute takes precedence over text content
			String url = el.attr(HREF);
			if (url == null || url.isEmpty()) {
				url = el.attr(SRC);
			}
			if (url != null || !url.isEmpty()) {
				
			}
			el.text();
		}

		private void checkTypeof(Element el, int depth) {
			String type = el.attr(TYPEOF);
			if (type == null || type.isEmpty()) {
				return;
			}
			IRI obj = createIRI(resource);
			createTriple(, RDF.TYPE, obj);
		}

		@Override
		public void head(Node node, int i) {
			if (node instanceof Element) {
				Element el = (Element) node;

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
				// remove out-of-scope namespaces 
				localNS.remove(i);
				localSubject.remove(i);
			}
		}
	}
}
