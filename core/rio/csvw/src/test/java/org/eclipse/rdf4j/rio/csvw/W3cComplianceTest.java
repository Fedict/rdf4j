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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
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
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataNone;
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
	private static String TEST_BASE_URI = "http://www.w3.org/2013/csvw/tests/";
	private static int PORT = 8989;
	private static Server server;

	@BeforeAll
	public static void setup() throws Exception {
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

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { nsContext, webContext });
		server.setHandler(handlers);

		// context.setVirtualHosts(new String[] { "www.w3.org" });
		System.err.println("WEBDIR: " + webDir);
		server.start();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		server.stop();
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

		try {
			ParserConfig cfg = new ParserConfig();
			URL csv = testCase.getCSV();

			if (testCase.getJsonMetadata() != null) {
				cfg.set(CSVWParserSettings.METADATA_INPUT_MODE, true);
				cfg.set(CSVWParserSettings.METADATA_URL, testCase.getJsonMetadata().toString());

				try (InputStream is = testCase.getJsonMetadata().openStream()) {
					compareResults(testCase, cfg, csv.toString(), is);
				}
			} else {
				// basic tests without metadata file
				cfg.set(CSVWParserSettings.METADATA_INPUT_MODE, false);
				cfg.set(CSVWParserSettings.METADATA_PROVIDER, new CSVWMetadataNone());
				cfg.set(CSVWParserSettings.DATA_URL, testCase.getCSV().toString());

				int pos = csv.getPath().lastIndexOf("/tests/") + 7;
				String fname = csv.getPath().substring(pos);

				System.err.println("Test " + i + " : " + fname);
				try (InputStream is = testCase.getCSV().openStream()) {
					compareResults(testCase, cfg, csv.toString(), is);
				}
			}
		} catch (AssertionError e) {
			fail();
		}
	}

	/**
	 * Get classpath location for a file
	 *
	 * @param file
	 * @return
	 */
	private static URL getLocation(String file) {
		URL url = null;
		try {
			url = new URL(file);
		} catch (Exception e) {
			//
		}
		return url;
		// return W3cComplianceTest.class.getClassLoader().getResource("w3c/" + file);
	}

	/*
	 * @ParameterizedTest
	 *
	 * @MethodSource("data") public void parsingTest(URL testCasePath) throws IOException, InterruptedException {
	 * runParsingTest(testCasePath); }
	 *
	 */
	/**
	 * Get test file URLs from manifest file(s)
	 *
	 * @return
	 */
	private static List<W3CTest> getTestFiles() {
		List<W3CTest> tests = null;

		String nsMF = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#";
		String csvtMF = "http://www.w3.org/2013/csvw/tests/vocab#";

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
							Models.getPropertyIRIs(model, t, Values.iri(csvtMF, "implicit"), (Resource) null),
							Models.getPropertyIRI(model, t, Values.iri(nsMF, "result"), (Resource) null).orElse(null),
							!Models.getPropertyIRI(model, t, RDF.TYPE, (Resource) null)
									.orElse(null)
									.equals(Values.iri(csvtMF, "NegativeRdfTest")))
					)
					.collect(Collectors.toList());
		}
		return tests;
	}

	/* Test Object */
	static class W3CTest {
		String id;
		String name;
		IRI csv;
		Set<IRI> json;
		IRI result;
		boolean positive;

		/**
		 * Get URL of CSV data file
		 *
		 * @return
		 * @throws IOException
		 */
		public URL getCSV() throws IOException {
			if (csv == null) {
				return null;
			}
			return new URL(csv.toString());
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
			if (json == null || json.isEmpty()) {
				return null;
			}
			return new URL(json.stream().findFirst().get().toString());
		}

		public W3CTest(String id, Literal name, IRI csv, Set<IRI> json, IRI result, boolean positive) {
			this.id = id;
			this.name = name.stringValue();
			this.csv = csv;
			this.json = json;
			this.result = result;
			this.positive = positive;
		}
	}
}
