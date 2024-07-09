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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.csvw.parsers.Parser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Basic (experimental) CSV on the Web Parser
 *
 * @author Bart Hanssens
 * @see <a href="https://w3c.github.io/csvw/primer/">CSV on the Web Primer</a>
 *
 * @since 5.1.0
 */
public class CSVWParser extends AbstractRDFParser {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CSVWParser.class);

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.CSVW;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {

		clear();

		RDFHandler rdfHandler = getRDFHandler();

		Model metadata = parseMetadata(in, null, baseURI);
		if (metadata == null || metadata.isEmpty()) {
			throw new RDFParseException("No metadata found");
		}

		List<Value> tables = getTables(metadata);
		for (Value table : tables) {
			URI csvFile = getURL(metadata, (Resource) table, baseURI);
			if (csvFile == null) {
				throw new RDFParseException("Could not find URL");
			}
			metadata.getNamespaces().add(new SimpleNamespace("", csvFile.toString() + "#"));
			Resource tableSchema = getTableSchema(metadata, (Resource) table);
			List<Value> columns = getColumns(metadata, tableSchema);
			Parser[] cellParsers = columns.stream()
					.map(c -> getCellParser(metadata, (Resource) c))
					.collect(Collectors.toList())
					.toArray(new Parser[columns.size()]);

			parseCSV(metadata, rdfHandler, csvFile, cellParsers, (Resource) table);
		}
		clear();
	}

	@Override
	public void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		Model metadata = parseMetadata(null, reader, baseURI);

		clear();
	}

	/**
	 * Parse JSON-LD metadata
	 *
	 * @param in
	 * @param reader
	 * @param baseURI
	 * @return
	 * @throws IOException
	 */
	private Model parseMetadata(InputStream in, Reader reader, String baseURI) throws IOException {
		Model metadata = null;
		ParserConfig cfg = new ParserConfig();

		if (in != null) {
			metadata = Rio.parse(in, null, RDFFormat.JSONLD, cfg);
		}

//		if (reader != null) {
//			return Rio.parse(reader, baseURI, RDFFormat.JSONLD, cfg);
//		}
		return metadata;
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
	 *
	 * @param metadata
	 * @param table
	 * @return
	 */
	private Parser getCellParser(Model metadata, Resource column) {
		Parser parser = new Parser();

		Optional<Value> name = Models.getProperty(metadata, column, CSVW.NAME);
		if (!name.isPresent()) {
			throw new RDFParseException("Metadata file does not contain name for column " + column);
		}
		parser.setName(name.get().stringValue());

		Optional<Value> defaultVal = Models.getProperty(metadata, column, CSVW.DEFAULT);
		if (defaultVal.isPresent()) {
			parser.setDefaultValue(defaultVal.get().stringValue());
		}

		// Optional<Value> dataType = Models.getProperty(metadata, column, CSVW.DATATYPE);
		// parser.setDataType((IRI) dataType.orElse(XSD.STRING.getIri()));

		Optional<Value> propertyURL = Models.getProperty(metadata, column, CSVW.PROPERTY_URL);
		if (propertyURL.isPresent()) {
			parser.setPropertyURL(metadata.getNamespaces(), propertyURL.get().stringValue());
		}

		Optional<Value> valueURL = Models.getProperty(metadata, column, CSVW.VALUE_URL);
		if (valueURL.isPresent()) {
			parser.setValueURL(valueURL.get().stringValue());
		}
		return parser;
	}

	private IRI getDataType(Model metadata, Value col) {
		return XSD.STRING.getIri();
	}

	/**
	 * Get "about" URL template, to be used to create the subject of the triples
	 *
	 * @param metadata
	 * @param subject
	 * @return aboutURL or null
	 */
	private String getAboutURL(Model metadata, Resource subject) {
		return Models.getPropertyString(metadata, subject, CSVW.ABOUT_URL).orElse(null);
	}

	/**
	 * Get the index of the column name used to replace the placeholder value in the aboutURL
	 *
	 * @param aboutURL
	 * @param cellParsers
	 * @return 0-based index or -1
	 */
	private int getAboutIndex(String aboutURL, Parser[] cellParsers) {
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
	 * Parse a CSV file
	 *
	 * @param csvFile     URI of CSV file
	 * @param cellParsers cell parsers
	 * @param aboutURL
	 * @param aboutIndex
	 */
	private void parseCSV(Model metadata, RDFHandler handler, URI csvFile, Parser[] cellParsers, Resource table) {
		String aboutURL = getAboutURL(metadata, table);

		// check for placeholder / column name that's being used to create subject IRI
		int aboutIndex = getAboutIndex(aboutURL, cellParsers);
		String placeholder = (aboutIndex > -1) ? cellParsers[aboutIndex].getName() : null;

		LOGGER.info("Parsing {}", csvFile);
		long line = 0;
		try (InputStream is = csvFile.toURL().openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				CSVReader csv = getCSVReader(metadata, reader)) {

			String[] cells;
			while ((cells = csv.readNext()) != null) {
				Resource subject = getIRIorBnode(cellParsers, cells, aboutURL, aboutIndex, placeholder);
				Value val;
				Statement stmt;
				for (int i = 0; i < cells.length; i++) {
					if (i == aboutIndex) { // already processed to get subject
						continue;
					}
					IRI predicate = cellParsers[i].getPropertyIRI();
					val = cellParsers[i].parse(cells[i]);
					handler.handleStatement(Statements.statement(subject, predicate, val, null));
				}
				line++;
			}
		} catch (IOException | CsvValidationException ex) {
			throw new RDFParseException("Error parsing " + csvFile, ex, line, -1);
		}
	}

	private CSVReader getCSVReader(Model metadata, Reader reader) {
		CSVParser parser = new CSVParserBuilder().build();
		return new CSVReaderBuilder(reader).withSkipLines(1).withCSVParser(parser).build();
	}

	/**
	 * Get subject IRI or blank node
	 *
	 * @param cellParsers
	 * @param aboutURL
	 * @param aboutIndex
	 */
	private Resource getIRIorBnode(Parser[] cellParsers, String[] cells, String aboutURL, int aboutIndex,
			String placeholder) {
		if (aboutIndex > -1) {
			Value val = cellParsers[aboutIndex].parse(cells[aboutIndex]);
			if (val != null) {
				return Values.iri(aboutURL.replace(placeholder, val.toString()));
			} else {
				throw new RDFParseException("NULL value in aboutURL");
			}
		} else {
			return Values.bnode();
		}
	}
}
