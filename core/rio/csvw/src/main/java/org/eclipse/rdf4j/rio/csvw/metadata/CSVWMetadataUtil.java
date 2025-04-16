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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.csvw.CSVWParserSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;

/**
 * Utility class for CSVW metadata
 *
 * @author Bart.Hanssens
 */
public class CSVWMetadataUtil {

	private static final ParserConfig METADATA_CFG = new ParserConfig().set(JSONLDSettings.WHITELIST,
			Set.of("http://www.w3.org/ns/csvw", "https://www.w3.org/ns/csvw", "https://www.w3.org/ns/csvw.jsonld"));

	/**
	 * Get the JSON-LD metadata as an RDF model
	 *
	 * @param provider
	 * @return
	 * @throws IOException
	 */
	public static Model getMetadataAsModel(CSVWMetadataProvider provider) throws IOException {
		Model m = null;
		InputStream minput = (provider != null) ? provider.getMetadata() : null;

		if (minput != null) {
			byte[] bytes = minput.readAllBytes();
			String str = new String(bytes, StandardCharsets.UTF_8);
			try (InputStream s = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))) {
				m = Rio.parse(s, null, RDFFormat.JSONLD, METADATA_CFG);
			}
		} // else {
			// LOGGER.warn("Metadata could not be found");
			// }
		return (m != null) ? m : new LinkedHashModel();
	}

	/**
	 * Get dialect node if present
	 *
	 * @param m
	 * @param table
	 * @return or null
	 */
	private static Resource getDialect(Model m, Resource table) {
		if (m == null || table == null) {
			return null;
		}
		Optional<Value> val = Models.getProperty(m, table, CSVW.HAS_DIALECT);
		if (!val.isPresent()) {
			// not on table, maybe at root level
			val = Models.object(m.filter(null, CSVW.HAS_DIALECT, null));
		}
		return val.isPresent() ? (Resource) val.get() : null;
	}

	/**
	 * Get the CSV dialect from metadata configuration as a map of IRIs and values
	 *
	 * @param m     RDF model
	 * @param table
	 * @return map with dialect configuration
	 */
	public static Map<IRI, Object> getDialectConfig(Model m, Resource table) {
		Map<IRI, Object> map = new HashMap<>();

		// use dummy model / node to retrieve default values
		Model metadata = (m == null) ? new LinkedHashModel() : m;
		Resource dialect = getDialect(metadata, table);
		if (dialect == null) {
			dialect = Values.bnode();
		}

		map.put(CSVW.ENCODING,
				Models.getPropertyString(metadata, dialect, CSVW.ENCODING).orElse("utf-8"));
		map.put(CSVW.HEADER,
				Boolean.valueOf(Models.getPropertyString(metadata, dialect, CSVW.HEADER).orElse("true")));
		map.put(CSVW.HEADER_ROW_COUNT, (boolean) map.get(CSVW.HEADER)
				? Integer.valueOf(Models.getPropertyString(metadata, dialect, CSVW.HEADER_ROW_COUNT).orElse("1"))
				: 0);
		map.put(CSVW.SKIP_ROWS,
				Integer.valueOf(Models.getPropertyString(metadata, dialect, CSVW.SKIP_ROWS).orElse("0")));
		map.put(CSVW.DELIMITER,
				Models.getPropertyString(metadata, dialect, CSVW.DELIMITER).orElse(","));
		map.put(CSVW.QUOTE_CHAR,
				Models.getPropertyString(metadata, dialect, CSVW.QUOTE_CHAR).orElse("\""));
		map.put(CSVW.DOUBLE_QUOTE,
				Models.getPropertyString(metadata, dialect, CSVW.DOUBLE_QUOTE).orElse("\\"));
		map.put(CSVW.TRIM,
				Boolean.valueOf(Models.getPropertyString(metadata, dialect, CSVW.TRIM).orElse("true")));

		return map;
	}

	/**
	 * Get metadata that does not help parsing the CSVW, but is included anyway.
	 *
	 * E.g last update, license, long descriptions...
	 *
	 * @param m
	 * @return
	 */
	public static Model getExtraMetadata(Model m, Resource rootNode, IRI predicate) {
		Model extra = new LinkedHashModel();
		Resource oldRoot = null;

		if (predicate != null) {
			Iterable<Statement> roots = m.getStatements(null, predicate, null);
			if (roots.iterator().hasNext()) {
				oldRoot = roots.iterator().next().getSubject();
			}
		}
		if (oldRoot != null) {
			m.getStatements(oldRoot, null, null).forEach(s -> {
				IRI p = s.getPredicate();
				if (!p.getNamespace().equals(CSVW.NAMESPACE) || p.equals(CSVW.NOTE)) {
					Value obj = s.getObject();
					extra.add(Statements.statement(rootNode, p, obj, null));
					if (obj instanceof Resource) {
						Iterable<Statement> second = m.getStatements((Resource) obj, null, null);
						second.forEach(s2 -> {
							extra.add(s2);
							Value obj2 = s2.getObject();
							if (obj2 instanceof Resource) {
								Iterable<Statement> third = m.getStatements((Resource) obj2, null, null);
								third.forEach(s3 -> extra.add(s3));
							}
						});
					}
				}
			});
		}
		return extra;
	}
}
