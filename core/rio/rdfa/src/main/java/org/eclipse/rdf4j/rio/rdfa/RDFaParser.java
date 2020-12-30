/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import java.util.HashSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.rdfa.util.XHTMLNodeVisitor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.NodeTraversor;

/**
 * RDF parser for RDFa files.
 * 
 * @author Bart Hanssens
 * @since 3.6.0
 */
public class RDFaParser extends AbstractRDFParser implements RDFParser {
	protected String baseURL = "";

	/**
	 * Creates a new RDFaParser that will use a {@link SimpleValueFactory} to create object for resources, bNodes and
	 * literals.
	 */
	public RDFaParser() {
		super();
	}

	/**
	 * Creates a new RDFaParser that will use the supplied {@link ValueFactory} to create RDF model objects.
	 * 
	 * @param valueFactory A ValueFactory.
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
			NodeTraversor.traverse(new XHTMLNodeVisitor(this, baseURI, doc.root()), doc);
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
			}
		}) {
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
	public void handleNS(String prefix, String ns) {
		rdfHandler.handleNamespace(prefix, ns);
	}

	/**
	 * Handle a triple statement
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 */
	public void handleTriple(Resource subj, IRI pred, Value obj) {
		rdfHandler.handleStatement(valueFactory.createStatement(subj, pred, pred));
	}

	/**
	 * Handle an error
	 * 
	 * @param msg
	 * @throws RDFParseException
	 */
	public void error(String msg) throws RDFParseException {
		reportError(msg, null);
	}

	/**
	 * Handle a fatal error
	 * 
	 * @param msg
	 * @throws RDFParseException
	 */
	public void fatalError(String msg) throws RDFParseException {
		reportFatalError(msg);
	}

	public IRI createIRI(String str) {
		return createURI(str);
	}

	public Resource createBlank() {
		return createNode();
	}
}
