/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.csvw.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for CSVW m
 *
 * @author Bart.Hanssens
 */
public class CSVWMetadataUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWMetadataUtil.class);

	protected static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.proxy(ProxySelector.getDefault())
			.build();

	private static final ParserConfig METADATA_CFG = new ParserConfig().set(JSONLDSettings.WHITELIST,
			Set.of("http://www.w3.org/ns/csvw", "https://www.w3.org/ns/csvw",
					"https://www.w3.org/ns/csvw.jsonld", "https://schema.org", "http://schema.org/"));

	/**
	 * Get the JSON-LD m as an RDF model
	 *
	 * @param provider
	 * @return
	 * @throws IOException
	 */
	public static Model getMetadataAsModel(CSVWMetadataProvider provider) throws IOException {
		Model m = null;

		InputStream minput = (provider != null) ? provider.getMetadata() : null;
		if (minput != null) {
			m = Rio.parse(minput, null, RDFFormat.JSONLD, METADATA_CFG);
		}
		return (m != null) ? m : new LinkedHashModel();
	}

	/**
	 * Get dialect node if present
	 *
	 * @param metadata
	 * @param table
	 * @return or null
	 */
	private static Resource getDialect(Model metadata, Resource table) {
		if (metadata == null || table == null) {
			return null;
		}
		Optional<Value> val = Models.getProperty(metadata, table, CSVW.HAS_DIALECT);
		if (!val.isPresent()) {
			// not on table, maybe at root level
			val = Models.object(metadata.filter(null, CSVW.HAS_DIALECT, null));
		}
		return val.isPresent() ? (Resource) val.get() : null;
	}

	/**
	 * Get the CSV dialect from m configuration as a map of IRIs and values
	 *
	 * @param metadata RDF model
	 * @param table
	 * @return map with dialect configuration
	 */
	public static Map<IRI, Object> getDialectConfig(Model metadata, Resource table) {
		Map<IRI, Object> map = new HashMap<>();

		// use dummy model / node to retrieve default values
		Model m = (metadata == null) ? new LinkedHashModel() : metadata;
		Resource dialect = getDialect(m, table);
		if (dialect == null) {
			dialect = Values.bnode();
		}
		map.put(CSVW.ENCODING,
				Models.getPropertyString(m, dialect, CSVW.ENCODING).orElse("utf-8"));
		map.put(CSVW.HEADER,
				Boolean.valueOf(Models.getPropertyString(m, dialect, CSVW.HEADER).orElse("true")));
		map.put(CSVW.HEADER_ROW_COUNT,
				Integer.valueOf(Models.getPropertyString(m, dialect, CSVW.HEADER_ROW_COUNT).orElse("1")));
		map.put(CSVW.SKIP_ROWS,
				Integer.valueOf(Models.getPropertyString(m, dialect, CSVW.SKIP_ROWS).orElse("0")));
		map.put(CSVW.DELIMITER,
				Models.getPropertyString(m, dialect, CSVW.DELIMITER).orElse(","));
		map.put(CSVW.QUOTE_CHAR,
				Models.getPropertyString(m, dialect, CSVW.QUOTE_CHAR).orElse("\""));
		map.put(CSVW.DOUBLE_QUOTE,
				Models.getPropertyString(m, dialect, CSVW.DOUBLE_QUOTE).orElse("\\"));
		map.put(CSVW.TRIM,
				Models.getPropertyString(m, dialect, CSVW.TRIM).orElse("true"));
		map.put(CSVW.SKIP_INITIAL_SPACE,
				Boolean.valueOf(Models.getPropertyString(m, dialect, CSVW.SKIP_INITIAL_SPACE).orElse("true")));
		return map;
	}

	/**
	 * Get metadata that does not help parsing the CSVW, but is included for documentation purposes. E.g last update,
	 * license, long descriptions...
	 *
	 * @param metadata
	 * @param oldRoot
	 * @param newRoot
	 * @return
	 */
	public static Model getComments(Model metadata, Resource oldRoot, Resource newRoot) {
		Model extra = new LinkedHashModel();
		if (newRoot == null) {
			return extra;
		}

		// extra metadata, can be anything
		Iterable<Statement> statements = metadata.getStatements(oldRoot, null, null);
		for (Statement s : statements) {
			IRI p = s.getPredicate();
			if (!p.getNamespace().equals(CSVW.NAMESPACE) || p.equals(CSVW.NOTE)) {
				Value obj = s.getObject();
				extra.add(Statements.statement(newRoot, p, obj, null));
			}
		}

		// get metadata regardless how deep the statements are nasted,
		// but avoid looping forever by keeping track of processed subjects
		Set<Resource> subjects = new HashSet<>();
		subjects.add(oldRoot);

		boolean newStatements;
		do {
			newStatements = false;
			Set<Value> values = Set.copyOf(extra.objects());

			for (Value val : values) {
				if (val instanceof Resource && !subjects.contains((Resource) val)) {
					Resource res = (Resource) val;
					newStatements = true;
					subjects.add(res);
					statements = metadata.getStatements(res, null, null);

					for (Statement s : statements) {
						IRI p = s.getPredicate();
						if (!p.getNamespace().equals(CSVW.NAMESPACE) || p.equals(CSVW.NOTE)) {
							Value obj = s.getObject();
							extra.add(Statements.statement(res, p, obj, null));
						}
					}
				}
			}
		} while (newStatements);

		return extra;
	}

	/**
	 * Get the root subject of the metadata file
	 *
	 * @param metadata
	 * @return
	 */
	public static Resource getRootSubject(Model metadata) {
		Model m = metadata.filter(null, CSVW.HAS_DIALECT, null);
		if (m.isEmpty()) {
			m = metadata.filter(null, CSVW.HAS_TABLE, null);
		}
		if (m.isEmpty()) {
			m = metadata.filter(null, CSVW.TABLE_SCHEMA, null);
		}
		return m.subjects().stream().findFirst().orElse(null);
	}

	/**
	 * Get table subject (ID) if it is an URI
	 *
	 * @param rootSubject
	 * @param tableSubject
	 * @return
	 */
	public static Resource getTableSubject(Resource rootSubject, Resource tableSubject) {
		if (tableSubject != null && tableSubject.isIRI()) {
			return tableSubject;
		}
		if (rootSubject != null && rootSubject.isIRI()) {
			return rootSubject;
		}
		return Values.bnode();
	}

	/**
	 * Get the (blank node of the) tableschema for a given table
	 *
	 * @param metadata
	 * @param table
	 * @return
	 * @throws RDFParseException
	 */
	public static Resource getTableSchema(Model metadata, Resource table) {
		return Models.getPropertyResource(metadata, table, CSVW.TABLE_SCHEMA).orElse(null);
	}

	/**
	 * Get (the blank nodes of) the table(s)
	 *
	 * @param metadata
	 * @return
	 */
	public static List<Resource> getTables(Model metadata) throws RDFParseException {
		Iterator<Statement> it = metadata.getStatements(null, CSVW.HAS_TABLE, null).iterator();
		if (!it.hasNext()) {
			// only one table, simplified structure
			it = metadata.getStatements(null, CSVW.TABLE_SCHEMA, null).iterator();
			if (!it.hasNext()) {
				throw new RDFParseException("Metadata file has no tables and no tableSschema");
			}
			return List.of(it.next().getSubject());
		}
		List<Resource> tables = new ArrayList<>();
		while (it.hasNext()) {
			Resource table = (Resource) it.next().getObject();
			// check if whole table is to be suppressed
			Value val = Models.getProperty(metadata, table, CSVW.SUPPRESS_OUTPUT).orElse(Values.literal(false));
			boolean suppressed = Boolean.parseBoolean(val.stringValue());
			if (!suppressed) {
				tables.add(table);
			}
		}
		return tables;
	}

	/**
	 * Get the (blank nodes of the) columns for a given tableschema
	 *
	 * @param metadata
	 * @param tableSchema
	 * @return list of blank nodes
	 * @throws RDFParseException
	 */
	public static List<Resource> getColumns(Model metadata, Resource tableSchema) throws RDFParseException {
		Optional<Resource> head = Models.getPropertyResource(metadata, tableSchema, CSVW.COLUMN);
		if (!head.isPresent()) {
			throw new RDFParseException("Metadata file does not contain columns for " + tableSchema);
		}
		return RDFCollections.asValues(metadata, head.get(), new ArrayList<>())
				.stream()
				.map(c -> (Resource) c)
				.collect(Collectors.toList());
	}

	/**
	 * Try to open URL, silently fail if there is an error
	 *
	 * @param uri
	 * @return
	 */
	protected static byte[] tryURI(URI uri) {
		HttpRequest httpGet = HttpRequest.newBuilder().uri(uri).GET().build();

		CompletableFuture<HttpResponse<byte[]>> future = HTTP_CLIENT.sendAsync(httpGet,
				HttpResponse.BodyHandlers.ofByteArray());
		try {
			HttpResponse<byte[]> response = future.get();
			if (response.statusCode() == 200) {
				LOGGER.info("Opened URL on {}", uri);
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
