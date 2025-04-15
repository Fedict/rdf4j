/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.rio.csvw;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataFinder;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataLocation;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataProvider;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Largely based upon the SHACL W3C Compliance Test (courtesy of Håvard M.Ottestad)
 *
 */
public class W3cComplianceTest {
	private final static String nsMF = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#";
	private final static String csvtMF = "http://www.w3.org/2013/csvw/tests/vocab#";

	private final static String TEST_BASE_URI = "http://www.w3.org/2013/csvw/tests/";
	private final static int PORT = 8989;
	private static Server server;
	private static HandlerList handlerList;

	private static String proxyHost;
	private static String proxyPort;

	@BeforeAll
	public static void setup() throws Exception {
		// route all request to embedded Jetty
		proxyHost = System.getProperty("http.proxyHost");
		proxyPort = System.getProperty("http.proxyPort");

		System.setProperty("http.proxyHost", "localhost");
		System.setProperty("http.proxyPort", String.valueOf(PORT));

		server = new Server(PORT);

		String webDir = W3cComplianceTest.class.getClassLoader().getResource("w3c/").toExternalForm();
		ResourceHandler webHandler = new ResourceHandler();
		webHandler.setResourceBase(webDir);
		ContextHandler webContext = new ContextHandler("/2013/csvw/tests");
		webContext.setHandler(webHandler);

		String nsDir = W3cComplianceTest.class.getClassLoader().getResource("ns/").toExternalForm();
		ResourceHandler nsHandler = new ResourceHandler();
		nsHandler.setResourceBase(nsDir);
		ContextHandler nsContext = new ContextHandler("/ns");
		nsContext.setHandler(nsHandler);

		handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { nsContext, webContext, new DefaultHandler() });
		server.setHandler(handlerList);

		server.start();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (proxyHost != null) {
			System.setProperty("http.proxyHost", proxyHost);
			System.setProperty("http.proxyPort", proxyPort);
		} else {
			System.clearProperty("http.proxyHost");
			System.clearProperty("http.proxyPort");

		}
		if (server != null) {
			server.stop();
		}
	}

	private void compareResults(W3CTest testCase, ParserConfig cfg, String baseURI, InputStream is) throws IOException {
		WriterConfig ttlcfg = new WriterConfig();
		ttlcfg.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		ttlcfg.set(BasicWriterSettings.PRETTY_PRINT, true);

		Model expected = testCase.getExpected();
		Model result = Rio.parse(is, baseURI, RDFFormat.CSVW, cfg, (Resource) null);

		if (testCase.positive) {
			try {
				assertTrue(Models.isomorphic(result, expected), testCase.name);
			} catch (Error e) {
				StringWriter w = new StringWriter();
				w.write("\nResult\n");
				Rio.write(result, w, RDFFormat.TURTLE, ttlcfg);
				w.write("\nExpected\n");
				Rio.write(expected, w, RDFFormat.TURTLE, ttlcfg);
				System.out.println(w.toString());
				throw (e);
			}
		} else {
			assertFalse(Models.isomorphic(result, expected), testCase.name);
		}
	}

	@ParameterizedTest
	@MethodSource("getTestFiles")
	public void test(W3CTest testCase) throws IOException {
		int i = 1;

		if (testCase.link != null) {
			HandlerWrapper wrapper = new HandlerWrapper() {
				@Override
				public void handle(String target, Request baseRequest, HttpServletRequest request,
						HttpServletResponse response) throws ServletException, IOException {
					System.err.println("wrapped");
					response.setHeader("Link", testCase.link);
					super.handle(target, baseRequest, request, response);
				}
			};

			System.err.println(testCase.link);
			HandlerList handlerListWrapped = new HandlerList();
			Handler[] oldHandlers = handlerList.getHandlers();
			System.err.println("Oldhandler " + oldHandlers[1].getClass().getName());
			wrapper.setHandler(oldHandlers[1]);
			handlerListWrapped.setHandlers(new Handler[] { oldHandlers[0], wrapper, oldHandlers[2] });
			server.setHandler(handlerListWrapped);
		}

		try {
			ParserConfig cfg = new ParserConfig();
			URL csv = testCase.getCSV();

			if (testCase.getJsonMetadata() != null) {
				// CSVWMetadataLocation metadataLocation = new
				// CSVWMetadataLocation(testCase.getJsonMetadata().toString());
				cfg.set(CSVWParserSettings.METADATA_INPUT_MODE, true);
				// cfg.set(CSVWParserSettings.METADATA_PROVIDER, metadataFinder);
				cfg.set(CSVWParserSettings.METADATA_URL, testCase.getJsonMetadata().toString());
				cfg.set(CSVWParserSettings.MINIMAL_MODE, testCase.isMinimal());

				try (InputStream is = testCase.getJsonMetadata().openStream()) {
					compareResults(testCase, cfg, TEST_BASE_URI, is);
				}
			} else {
				CSVWMetadataProvider meta = (testCase.metadata == null)
						? new CSVWMetadataFinder(testCase.getCSV())
						: new CSVWMetadataLocation(new URL(testCase.metadata));
				// basic tests, possibly without metadata file
				cfg.set(CSVWParserSettings.METADATA_INPUT_MODE, false);
				cfg.set(CSVWParserSettings.METADATA_PROVIDER, meta);
				// cfg.set(CSVWParserSettings.DATA_URL, testCase.getCSV().toString());

				int pos = csv.getPath().lastIndexOf("/tests/") + 7;
				String fname = csv.getPath().substring(pos);

				System.err.println("Test " + i + " : " + fname);
				try (InputStream is = testCase.getCSV().openStream()) {
					compareResults(testCase, cfg, testCase.getCSV().toString(), is);
				}
			}
		} catch (AssertionError e) {
			fail();
		}
		if (testCase.link != null) {
			server.setHandler(handlerList);
		}
	}

	/*
	 * @ParameterizedTest
	 *
	 * @MethodSource("data") public void parsingTest(URL testCasePath) throws IOException, InterruptedException {
	 * runParsingTest(testCasePath); }
	 *
	 */

	private static Value option(Model model, Resource t, String option) {
		Optional<Resource> node = Models.getPropertyResource(model, t, Values.iri(csvtMF, "option"), (Resource) null);
		if (node.isPresent()) {
			Optional<Value> val = Models.getProperty(model, node.get(), Values.iri(csvtMF, option), (Resource) null);
			if (val.isPresent()) {
				return val.get();
			}
		}
		return null;
	}

	private static boolean optionMinimal(Model model, Resource t) {
		Value val = option(model, t, "minimal");
		if (val != null) {
			return Boolean.parseBoolean(val.stringValue());
		}
		return false;
	}

	private static String optionMetadata(Model model, Resource t) {
		Value val = option(model, t, "metadata");
		return (val != null) ? val.stringValue() : null;
	}

	/**
	 * Get test file URLs from manifest file(s)
	 *
	 * @return
	 */
	private static List<W3CTest> getTestFiles() {
		List<W3CTest> tests = null;

		URL url = W3cComplianceTest.class.getClassLoader().getResource("w3c/manifest-rdf.ttl");

		if (url != null) {
			Model model;
			try {
				model = Rio.parse(url.openStream(), TEST_BASE_URI, RDFFormat.TURTLE, (Resource) null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Statement manifest = model.getStatements(null, RDF.TYPE, Values.iri(nsMF, "Manifest")).iterator().next();
			Statement header = model.getStatements(manifest.getSubject(), Values.iri(nsMF, "entries"), null)
					.iterator()
					.next();

			List<Value> entries = RDFCollections.asValues(model, (Resource) header.getObject(), new ArrayList<>());
			tests = entries.stream()
					.map(t -> (Resource) t)
					.map(t -> new W3CTest(
							t.stringValue(),
							Models.getPropertyLiteral(model, t, Values.iri(nsMF, "name"), (Resource) null).orElse(null),
							Models.getPropertyIRI(model, t, Values.iri(nsMF, "action"), (Resource) null).orElse(null),
							// Models.getPropertyIRIs(model, t, Values.iri(csvtMF, "implicit"), (Resource) null),
							Models.getPropertyIRI(model, t, Values.iri(nsMF, "result"), (Resource) null).orElse(null),
							Models.getPropertyString(model, t, Values.iri(csvtMF, "httpLink"), (Resource) null)
									.orElse(null),
							!Models.getPropertyIRI(model, t, RDF.TYPE, (Resource) null)
									.orElse(null)
									.equals(Values.iri(csvtMF, "NegativeRdfTest")),
							optionMinimal(model, t),
							optionMetadata(model, t)
					)
					)
					.collect(Collectors.toList());
		}
		return tests;
	}

	/* Test Object */
	static class W3CTest {
		String id;
		String name;
		IRI input;
		IRI result;
		String link;
		boolean positive;
		boolean minimal;
		String metadata;

		/**
		 * Get URL of CSV data file
		 *
		 * @return
		 * @throws IOException
		 */
		public URL getCSV() throws IOException {
			if (input == null || !input.toString().endsWith("csv")) {
				return null;
			}
			return new URL(input.toString());
		}

		/**
		 * Get expected triples as model
		 *
		 * @return
		 * @throws IOException
		 */
		public Model getExpected() throws IOException {
			if (result == null) {
				return new LinkedHashModel();
			}
			URL url = new URL(result.toString());
			try (InputStream is = url.openStream()) {
				return Rio.parse(is, url.toString(), RDFFormat.TURTLE, (Resource) null);
			}
		}

		/**
		 * Get URL of JSON metadata file
		 *
		 * @return
		 * @throws IOException
		 */
		public URL getJsonMetadata() throws IOException {
			if (input == null || !input.toString().endsWith("json")) {
				return null;
			}
			return new URL(input.toString());
		}

		public boolean isMinimal() {
			return minimal;
		}

		public W3CTest(String id, Literal name, IRI input, IRI result, String link,
				boolean positive, boolean minimal, String metadata) {
			this.id = id;
			this.name = name.stringValue();
			this.input = input;
			this.result = result;
			this.link = link;
			this.positive = positive;
			this.minimal = minimal;
			this.metadata = metadata;
		}
	}
}
