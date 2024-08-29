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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Based upon the SHACL W3C Compliance Test
 *
 */
public class W3cComplianceTest {

	@ParameterizedTest
	@MethodSource("getTestFiles")
	public void test(W3CTest testCase) throws IOException {
		if (!testCase.positive) {
			System.err.println("test should fail");
			return;
		}
		try {
			Model expected = testCase.getExpected();
			System.err.println("parsing " + testCase.getJson().toString());
			try (InputStream is = testCase.getJson().openStream()) {
				Model result = Rio.parse(is, RDFFormat.CSVW, (Resource) null);
				assertTrue(Models.isomorphic(result, expected), testCase.name);
			}
			System.err.println("done");
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
		return W3cComplianceTest.class.getClassLoader().getResource("w3c/" + file);
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

		URL url = getLocation("manifest-rdf.ttl");

		if (url != null) {
			Model model;
			try {
				model = Rio.parse(url.openStream(), getLocation("").toString(), RDFFormat.TURTLE, (Resource) null);
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
							Models.getPropertyIRI(model, t, Values.iri(csvtMF, "implicit"), (Resource) null)
									.orElse(null),
							Models.getPropertyIRI(model, t, Values.iri(nsMF, "action"), (Resource) null).orElse(null),
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
		IRI json;
		IRI result;
		boolean positive;

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
		public URL getJson() throws IOException {
			if (json == null) {
				return null;
			}
			return new URL(json.toString());
		}

		public W3CTest(String id, Literal name, IRI csv, IRI json, IRI result, boolean positive) {
			this.id = id;
			this.name = name.stringValue();
			this.csv = csv;
			this.json = json;
			this.result = result;
			this.positive = positive;
		}
	}
}
