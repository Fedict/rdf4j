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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataProvider;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataNone;

/**
 * Experimental CSV on the Web parser.
 *
 * @author Bart Hanssens
 *
 *         Basically it consists of an existing CSV file and a metadata file (in JSON-LD) describing the columns.
 *         Parsers need to convert the data client-side.
 *
 * @see <a href="https://w3c.github.io/csvw/primer/">CSV on the Web Primer</a>
 * @see <a href="https://w3c.github.io/csvw/syntax/">Model for Tabular Data and Metadata on the Web</a>
 * @see <a href="https://w3c.github.io/csvw/metadata">Metadata Vocabulary for Tabular Data</a>
 *
 * @since 5.1.0
 */
public class CSVWParser extends AbstractRDFParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWParser.class);

	private static final ParserConfig METADATA_CFG = new ParserConfig().set(JSONLDSettings.WHITELIST,
			Set.of("http://www.w3.org/ns/csvw", "https://www.w3.org/ns/csvw", "https://www.w3.org/ns/csvw.jsonld"));

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.CSVW;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {

		clear();

		Model metadata = getMetadataAsModel(in);

		rdfHandler = getRDFHandler();

		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);
		Resource rootNode = minimal ? null : generateTablegroupNode(rdfHandler);

		List<Value> tables = getTables(metadata);
		for (Value table : tables) {
			URI csvFile = getURL(metadata, (Resource) table, baseURI);
			if (csvFile == null) {
				throw new RDFParseException("Could not find URL");
			}
			Resource tableNode = minimal ? null : generateTableNode(rdfHandler, rootNode);
			// add dummy namespace for resolving unspecified column names / predicates relative to CSV file
			metadata.getNamespaces().add(new SimpleNamespace("_local", csvFile.toString() + "#"));

			Resource tableSchema = getTableSchema(metadata, (Resource) table);
			List<Value> columns = getColumns(metadata, tableSchema);
			CellParser[] cellParsers = columns.stream()
					.map(c -> CSVWUtil.getCellParser(metadata, (Resource) c))
					.collect(Collectors.toList())
					.toArray(new CellParser[columns.size()]);

			parseCSV(metadata, rdfHandler, csvFile, cellParsers, (Resource) table, tableNode);
		}
		clear();
	}

	@Override
	public void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		throw new IOException("not implemented yet");
//		Model metadata = parseMetadata(null, reader, baseURI);
	}

	/**
	 * Get the JSON-LD metadata as an RDF model
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private Model getMetadataAsModel(InputStream in) throws IOException {
		Model m = null;
		InputStream metadata = null;

		if (getParserConfig().get(CSVWParserSettings.METADATA_INPUT_MODE)) {
			metadata = in;
		} else {
			// input is CSV, so try to find associated metadata
			CSVWMetadataProvider provider = getParserConfig().get(CSVWParserSettings.METADATA_PROVIDER);
			if ((provider != null) && !(provider instanceof CSVWMetadataNone)) {
				metadata = provider.getMetadata();
			}
		}
		if (metadata != null) {
			byte[] bytes = metadata.readAllBytes();
			String str = new String(bytes, StandardCharsets.UTF_8);
			/*
			 * if (!str.contains("@context")) { str = "{\"@context\": \"https://www.w3.org/ns/csvw.jsonld\"," +
			 * str.substring(1); } System.err.println("METADATA JSON: " + str);
			 */
			try (InputStream s = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))) {
				m = Rio.parse(s, null, RDFFormat.JSONLD, METADATA_CFG);
			}
		}
		if (m == null) {
			LOGGER.warn("No metadata found");
			m = new LinkedHashModel();
		}
		return m;
	}

	/**
	 * Get the location of the CSV file
	 *
	 * @param metadata
	 * @param subject
	 * @param baseURI
	 */
	private URI getURL(Model metadata, Resource table, String baseURI) {
		Optional<String> val = Models.getPropertyString(metadata, table, CSVW.URL);
		if (val.isPresent()) {
			String s = val.get();
			if (s.startsWith("http")) {
				return URI.create(s);
			}
			if (baseURI != null) {
				return URI.create(baseURI).resolve(s);
			}
			return URI.create(s);
		}
		return null;
	}

	/**
	 * Get the (blank node of the) tableschema for a given table
	 *
	 * @param metadata
	 * @param table
	 * @return
	 * @throws RDFParseException
	 */
	private Resource getTableSchema(Model metadata, Resource table) throws RDFParseException {
		return Models.getPropertyResource(metadata, table, CSVW.TABLE_SCHEMA)
				.orElseThrow(() -> new RDFParseException("Metadata file does not contain tableSchema for " + table));
	}

	/**
	 * Get (the blank nodes of) the table(s)
	 *
	 * @param metadata
	 * @return
	 */
	private List<Value> getTables(Model metadata) throws RDFParseException {
		Iterator<Statement> it = metadata.getStatements(null, CSVW.TABLES, null).iterator();
		if (!it.hasNext()) {
			// only one table, simplified structure
			it = metadata.getStatements(null, CSVW.TABLE_SCHEMA, null).iterator();
			if (!it.hasNext()) {
				throw new RDFParseException("Metadata file has no tables and no tableSschema");
			}
			return List.of(it.next().getSubject());
		}
		return RDFCollections.asValues(metadata, (Resource) it.next().getObject(), new ArrayList<>());
	}

	/**
	 * Get the (blank nodes of the) columns for a given tableschema
	 *
	 * @param metadata
	 * @param tableSchema
	 * @return list of blank nodes
	 * @throws RDFParseException
	 */
	private List<Value> getColumns(Model metadata, Resource tableSchema) throws RDFParseException {
		Optional<Resource> head = Models.getPropertyResource(metadata, tableSchema, CSVW.COLUMN);
		if (!head.isPresent()) {
			throw new RDFParseException("Metadata file does not contain columns for " + tableSchema);
		}
		return RDFCollections.asValues(metadata, head.get(), new ArrayList<>());
	}

	/**
	 * Get "about" URL template, to be used to create the subject of the triples
	 *
	 * @param metadata
	 * @param subject
	 * @return aboutURL or null
	 */
	private String getAboutURL(Model metadata, Resource table) {
		return Models.getPropertyString(metadata, table, CSVW.ABOUT_URL)
				.orElse(Models.getPropertyString(metadata, getTableSchema(metadata, table), CSVW.ABOUT_URL)
						.orElse(null));
	}

	/**
	 * Get the index of the column name used to replace the placeholder value in the aboutURL
	 *
	 * @param aboutURL
	 * @param cellParsers
	 * @return 0-based index or -1
	 */
	private int getAboutIndex(String aboutURL, CellParser[] cellParsers) {
		if (aboutURL == null || aboutURL.isEmpty()) {
			return -1;
		}

		String s;
		for (int i = 0; i < cellParsers.length; i++) {
			s = cellParsers[i].getName();
			if (s != null && aboutURL.contains("{" + s + "}")) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Create tablegroup statement and return root node
	 *
	 * @param handler
	 * @return
	 */
	private Resource generateTablegroupNode(RDFHandler handler) {
		BNode node = Values.bnode();
		handler.handleStatement(Statements.statement(node, RDF.TYPE, CSVW.TABLE_GROUP, null));
		return node;
	}

	/**
	 * Create table statements and return table node
	 *
	 * @param handler
	 * @return
	 */
	private Resource generateTableNode(RDFHandler handler, Resource rootNode) {
		BNode node = Values.bnode();
		handler.handleStatement(Statements.statement(rootNode, CSVW.TABLES, node, null));
		handler.handleStatement(Statements.statement(node, RDF.TYPE, CSVW.TABLE, null));
		return node;
	}

	/**
	 * Create row statements and return row node
	 *
	 * @param handler
	 * @return
	 */
	private Resource generateRowNode(RDFHandler handler, Resource tableNode, Resource subject, long rownum) {
		BNode node = Values.bnode();
		handler.handleStatement(Statements.statement(tableNode, CSVW.HAS_ROW, node, null));
		handler.handleStatement(Statements.statement(node, RDF.TYPE, CSVW.ROW, null));
		handler.handleStatement(Statements.statement(node, CSVW.ROWNUM, Values.literal(rownum), null));
		handler.handleStatement(Statements.statement(node, CSVW.DESCRIBES, subject, null));
		return node;
	}

	/**
	 * Check which cellparsers have placeholders that need to be replaced
	 *
	 * @param cellParsers
	 * @return
	 */
	private boolean[] needReplacement(CellParser[] cellParsers) {
		boolean[] placeholders = new boolean[cellParsers.length];

		for (int i = 0; i < cellParsers.length; i++) {
			placeholders[i] = (cellParsers[i].getAboutPlaceholders().length > 0) ||
					(cellParsers[i].getValuePlaceholders().length > 0);
		}
		return placeholders;
	}

	/**
	 * Parse a CSV file
	 *
	 * @param csvFile     URI of CSV file
	 * @param cellParsers cell parsers
	 * @param aboutURL
	 * @param aboutIndex
	 */
	private void parseCSV(Model metadata, RDFHandler handler, URI csvFile, CellParser[] cellParsers, Resource table,
			Resource tableNode) {
		LOGGER.info("Parsing {}", csvFile);

		String aboutURL = getAboutURL(metadata, table);

		Charset encoding = CSVWUtil.getEncoding(metadata, table);
		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);

		// check for placeholder / column name that's being used to create subject IRI
		int aboutIndex = getAboutIndex(aboutURL, cellParsers);
		String placeholder = (aboutIndex > -1) ? cellParsers[aboutIndex].getNameEncoded() : null;

		// check which columns need replacement in aboutURL/valueURL
		boolean[] needReplacement = needReplacement(cellParsers);
		boolean doReplace = false;
		for (int i = 0; i < needReplacement.length; i++) {
			if (needReplacement[i]) {
				doReplace = true;
				break;
			}
		}

		long line = 1;
		try (InputStream is = csvFile.toURL().openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
				CSVReader csv = CSVWUtil.getCSVReader(metadata, table, reader)) {

			Map<String, String> values = null;
			String[] cells;

			while ((cells = csv.readNext()) != null) {
				Resource aboutSubject = getIRIorBnode(cellParsers, cells, aboutURL, aboutIndex, placeholder);
				Resource rowNode = minimal ? null : generateRowNode(rdfHandler, tableNode, aboutSubject, line);

				if (doReplace) {
					values = new HashMap<>(cells.length + 4, 1.0f);
					values.put("{_row}", Long.toString(line));
				}

				// csv cells
				for (int i = 0; i < cells.length; i++) {
					if (doReplace) {
						values.put("{_col}", Long.toString(i));
					}
					if (i == aboutIndex) { // already processed to get subject
						if (doReplace) {
							values.put(cellParsers[i].getNameEncoded(), cellParsers[i].parse(cells[i]).stringValue());
						}
						continue;
					}
					Value val = cellParsers[i].parse(cells[i]);
					if (doReplace) {
						values.put(cellParsers[i].getNameEncoded(), val.stringValue());
					}
					if (!cellParsers[i].isSuppressed() && !needReplacement[i]) {
						handler.handleStatement(buildStatement(cellParsers[i], cells[i], aboutSubject, val));
					}
				}
				// second pass, this time to retrieve replace placeholders in URLs with column values
				for (int i = 0; i < cells.length; i++) {
					if (i == aboutIndex || !needReplacement[i]) { // already processed to get subject
						continue;
					}
					if (!cellParsers[i].isSuppressed()) {
						handler.handleStatement(buildStatement(cellParsers[i], cells[i], aboutSubject, values));
					}
				}
				// virtual columns, if any
				for (int i = cells.length; i < cellParsers.length; i++) {
					if (doReplace) {
						values.put("{_col}", Long.toString(i));
					}
					handler.handleStatement(buildStatement(cellParsers[i], null, aboutSubject, values));
				}
				line++;
			}
		} catch (IOException | CsvValidationException ex) {
			throw new RDFParseException("Error parsing " + csvFile, ex, line, -1);
		}
	}

	/**
	 * Generate statement
	 *
	 * @param handler
	 * @param cellParser
	 * @param cells
	 * @param aboutSubject
	 */
	private Statement buildStatement(CellParser cellParser, String cell, Resource aboutSubject, Value val) {
		Resource s = cellParser.getAboutUrl(cell);
		IRI predicate = cellParser.getPropertyIRI();
		Resource o = cellParser.getValueUrl(cell);

		return Statements.statement((s != null) ? s : aboutSubject, predicate, (o != null) ? o : val, null);
	}

	/**
	 * Generate statement
	 *
	 * @param handler
	 * @param cellParser
	 * @param cells
	 * @param aboutSubject
	 */
	private Statement buildStatement(CellParser cellParser, String cell, Resource aboutSubject,
			Map<String, String> values) {
		Resource s = cellParser.getAboutUrl(values);
		IRI predicate = cellParser.getPropertyIRI();
		Value o = cellParser.getValueUrl(values, cell);
		if (o == null && cell != null) {
			o = cellParser.parse(cell);
		}

		return Statements.statement((s != null) ? s : aboutSubject, predicate, o, null);
	}

	/**
	 * Get subject IRI or blank node
	 *
	 * @param cellParsers
	 * @param aboutURL
	 * @param aboutIndex
	 */
	private Resource getIRIorBnode(CellParser[] cellParsers, String[] cells, String aboutURL, int aboutIndex,
			String placeholder) {
		if (aboutIndex > -1) {
			Value val = cellParsers[aboutIndex].parse(cells[aboutIndex]);
			if (val != null) {
				return Values.iri(aboutURL.replace(placeholder, val.stringValue()));
			} else {
				throw new RDFParseException("NULL value in aboutURL");
			}
		} else {
			return Values.bnode();
		}
	}
}
