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

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Håvard Ottestad
 */
public class ShaclTestWithoutSparqlValidationTest extends AbstractShaclTest {

	@Override
	SailRepository getShaclSail(TestCase testCase) {
		SailRepository sail = super.getShaclSail(testCase);
		ShaclSail shaclSail = (ShaclSail) sail.getSail();
		shaclSail.sparqlValidation = false;
		return sail;
	}

	@ParameterizedTest
	@MethodSource("testsToRunWithIsolationLevel")
	public void test(TestCase testCase, IsolationLevel isolationLevel) {
		runWithAutomaticLogging(() -> runTestCase(testCase, isolationLevel, false));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void testSingleTransaction(TestCase testCase) {
		runWithAutomaticLogging(() -> runTestCaseSingleTransaction(testCase));
	}

	@ParameterizedTest
	@MethodSource("testsToRunWithIsolationLevel")
	public void testRevalidation(TestCase testCase, IsolationLevel isolationLevel) {
		runWithAutomaticLogging(() -> runTestCaseRevalidate(testCase, isolationLevel));
	}

	@ParameterizedTest
	@MethodSource("testsToRunWithIsolationLevel")
	public void testNonEmpty(TestCase testCase, IsolationLevel isolationLevel) {
		runWithAutomaticLogging(() -> runTestCase(testCase, isolationLevel, true));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void testParsing(TestCase testCase) {
		runWithAutomaticLogging(() -> runParsingTest(testCase));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void testReferenceImplementation(TestCase testCase) {
		runWithAutomaticLogging(() -> referenceImplementationTestCaseValidation(testCase));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void testShaclValidator(TestCase testCase) {
		runWithAutomaticLogging(() -> runWithShaclValidator(testCase));
	}

}
