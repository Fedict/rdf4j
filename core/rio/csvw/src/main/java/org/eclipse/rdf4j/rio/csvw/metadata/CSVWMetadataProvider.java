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
package org.eclipse.rdf4j.rio.csvw.metadata;

import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide the JSON metadata for parsing the CSV data
 *
 * @author Bart Hanssens
 */
public abstract class CSVWMetadataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataProvider.class);

	protected static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.proxy(ProxySelector.getDefault())
			.build();

	/**
	 * Get the metadata as inputstream
	 *
	 * @return
	 */
	public abstract InputStream getMetadata();

	/**
	 * Try to open URL, silently fail if there is an error
	 *
	 * @param uri
	 * @return
	 */
	protected byte[] tryURI(URI uri) {
		HttpRequest httpGet = HttpRequest.newBuilder().uri(uri).GET().build();

		CompletableFuture<HttpResponse<byte[]>> future = HTTP_CLIENT.sendAsync(httpGet, BodyHandlers.ofByteArray());
		try {
			HttpResponse<byte[]> response = future.get();
			if (response.statusCode() == 200) {
				LOGGER.info("Using metadata found on {}", uri);
				return response.body();
			} else {
				LOGGER.debug("Could not open URL {}, received status {}", uri, response.statusCode());
			}
		} catch (ExecutionException ex) {
			LOGGER.error("Could not open URL {}", uri, ex);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

}
