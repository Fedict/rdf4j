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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Based upon the SHACL W3C Compliance Test
 *
 */
public class W3cComplianceTest {

	public static Stream<Arguments> data() {
		return getTestFiles().stream().sorted().map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void test(W3CTest testCase) throws IOException {
		try {
			runTest(testCase);
		} catch (AssertionError e) {
			throw e;
		}
	}

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
				model = Rio.parse(url.openStream(), RDFFormat.TRIG, (Resource) null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Value empty = Values.literal("");

			List<Value> entries = RDFCollections.asValues(model, Values.iri(nsMF, "entries"), new ArrayList<>(),
					(Resource) null);
			tests = entries.stream()
					.map(t -> (Resource) t)
					.map(t -> new W3CTest(
							t.stringValue(),
							Models.getProperty(model, t, Values.iri(nsMF, "name"), (Resource) null)
									.orElse(empty)
									.stringValue(),
							Models.getProperty(model, t, Values.iri(csvtMF, "action"), (Resource) null)
									.orElse(empty)
									.stringValue(),
							Models.getProperty(model, t, Values.iri(nsMF, "implicit"), (Resource) null)
									.orElse(empty)
									.stringValue(),
							Models.getProperty(model, t, Values.iri(nsMF, "result"), (Resource) null)
									.orElse(empty)
									.stringValue())
					)
					.collect(Collectors.toList());
		}
		return tests;
	}

	// Test Manifest
	static class W3CTest {
		String id;
		String name;
		String csv;
		String json;
		String result;

		public Model getExpected() throws IOException {
			URL location = getLocation(result);
			try (InputStream is = location.openStream()) {
				return Rio.parse(is, RDFFormat.TURTLE, (Resource) null);
			}
		}

		public URL getJson() throws IOException {
			return getLocation(json);
		}

		public W3CTest(String id, String name, String csv, String json, String result) {
			this.id = id;
			this.name = name;
			this.csv = csv;
			this.json = json;
			this.result = result;
		}
	}

	private void runTest(W3CTest test) throws IOException {
		Model expected = test.getExpected();

		try (InputStream is = test.getJson().openStream()) {
			Model result = Rio.parse(is, RDFFormat.CSVW, (Resource) null);
			assertTrue(Models.isomorphic(result, expected), test.name);
		}
	}
}
