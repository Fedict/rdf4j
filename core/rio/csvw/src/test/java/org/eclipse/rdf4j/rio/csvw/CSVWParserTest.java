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

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

/**
 *
 * @author Bart.Hanssens
 */
@ExtendWith(MockServerExtension.class)
public class CSVWParserTest extends AbstractTest {

	@Test
	public void testCSVWParser() throws IOException {
		Model model = new LinkedHashModel();
		CSVWParser parser = new CSVWParser();
		parser.setRDFHandler(new StatementCollector(model));
		parser.getParserConfig().set(BasicWriterSettings.BASE_DIRECTIVE, true);
		parser.parse(new FileInputStream("src/test/resources/painters-metadata.json"), getBase() + "/downloads/");

		System.err.println(model);
	}
}
