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
public abstract class AbstractTest {
	protected MockServerClient client;

	protected String getFile(String file) throws IOException {
		return new String(CSVWMetadataFinderTest.class.getResourceAsStream("/" + file).readAllBytes());
	}

	protected String getBase() {
		return "http://localhost:" + client.getPort() + "/";
	}

	@BeforeEach
	public void init(MockServerClient client) throws IOException {
		this.client = client;
		client.when(
				request().withMethod("GET").withPath("/downloads/painters.csv"))
				.respond(response().withBody(getFile("painters.csv")));
		client.when(
				request().withMethod("GET").withPath("/.well-known/csvm"))
				.respond(response().withBody(getFile("well-known-csvm")));
		client.when(
				request().withMethod("GET").withPath("/downloads/painters.csvm"))
				.respond(response().withBody(getFile("painters-metadata.json")));
	}
}
