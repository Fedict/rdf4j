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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.AbstractSimpleMarkupHandler;
import org.attoparser.simple.ISimpleMarkupHandler;
import org.attoparser.simple.ISimpleMarkupParser;
import org.attoparser.simple.SimpleMarkupParser;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

/**
 * RDF parser for RDFa files.
 * 
 * @author Bart.Hanssens
 */
public class RDFaParser extends AbstractRDFParser {
	public final static Map<String,String> INITIAL_CONTEXT = RDFaUtil.buildContext();

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
		try(InputStreamReader reader = new InputStreamReader(in)) {
			parse(reader, baseURI);
		}
	}

	@Override
	public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		ISimpleMarkupHandler handler = new RdfaMarkupHandler(baseURI, StandardCharsets.UTF_8);
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());

		if (this.rdfHandler != null) {
			this.rdfHandler.startRDF();
		}

		try {		
			parser.parse(reader, handler);
		} catch (ParseException ex) {
			reportFatalError(ex);
		}

		if (this.rdfHandler != null) {
			this.rdfHandler.endRDF();
		}
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		return result;
	}

	/**
	 * Parser
	 */
	private class RdfaMarkupHandler extends AbstractSimpleMarkupHandler {
		private Charset charset;
		private String baseURI;
		private int level = 0;

		/**
		 * Try to set character set from Content-Type string.
		 * Won't do anything if provided content type is null, empty, illegal or not supported.
		 * 
		 * @param content 
		 */
		private void setCharSet(String content) {
			if (content == null || content.isEmpty()) {
				return;
			}
			int i = content.toLowerCase().indexOf("charset=");
			String s = content.substring(i + "charset=".length());
			String chstr = s.split(";", 1)[0].replaceAll("'", "");
			try {
				charset = Charset.forName(chstr);
			} catch (IllegalArgumentException iae) {
							
			}
		}

		private void handleElement(String elementName, Map<String,String> attributes) {
			switch (elementName) {
				case "base":
					String href = attributes.getOrDefault("href", "");
					if (!href.isEmpty()) {
						baseURI = href;
					}
					break;
				case "meta":
					String equiv = attributes.get("http-equiv");
					if (equiv != null && equiv.toLowerCase().equals("content-type")) {
						String content = attributes.get("content");
						setCharSet(content);
					}
					break;
			}
		}
		
		@Override
		public void handleOpenElement​(String elementName, Map<String,String> attributes, int line, int col) {
			level++;
			handleElement(elementName, attributes);
		}

		@Override
		public void handleStandaloneElement​(String elementName, Map<String,String> attributes, boolean minimized, int line, int col) {
			handleElement(elementName, attributes);
		}

		@Override		
		public void handleCloseElement​(String elementName, int line, int col) {
			level--;
		}
		
		public RdfaMarkupHandler(String baseURI, Charset charset) {
			this.baseURI = (baseURI != null) ? baseURI : "";
			this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
		}
	}
}
