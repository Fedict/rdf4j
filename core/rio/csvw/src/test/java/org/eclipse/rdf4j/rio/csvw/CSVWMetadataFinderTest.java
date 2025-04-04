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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URI;

import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataFinder;
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
public class CSVWMetadataFinderTest extends AbstractTest {
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

	@Test
	public void testWellKnownLocation() throws IOException {
		URI uri = URI.create(getBase() + "downloads/painters.csv");

		String expected = getFile("painters-metadata.json");
		CSVWMetadataFinder finder = new CSVWMetadataFinder();
		finder.findByWellKnown(uri);
		String got = new String(finder.getMetadata().readAllBytes());
		assertEquals(expected, got);
	}
}
