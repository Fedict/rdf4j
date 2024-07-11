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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.CharSet;
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
import org.eclipse.rdf4j.rio.csvw.parsers.CellParser;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParserFactory;
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
			// add dummy namespace for resolving unspecified column names / predicates relative to CSV file
			metadata.getNamespaces().add(new SimpleNamespace("_local", csvFile.toString() + "#"));

			Resource tableSchema = getTableSchema(metadata, (Resource) table);
			List<Value> columns = getColumns(metadata, tableSchema);
			CellParser[] cellParsers = columns.stream()
					.map(c -> getCellParser(metadata, (Resource) c))
					.collect(Collectors.toList())
					.toArray(new CellParser[columns.size()]);

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
	 * Get parser for specific column
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	private CellParser getCellParser(Model metadata, Resource column) {
		IRI datatype = getDatatypeIRI(metadata, column);

		CellParser parser = CellParserFactory.create(datatype);

		Models.getPropertyString(metadata, column, CSVW.LANG).ifPresent(v -> parser.setLang(v));
		getFormat(metadata, column).ifPresent(v -> parser.setFormat(v.stringValue()));

		Models.getPropertyString(metadata, column, CSVW.NAME)
				.ifPresentOrElse(v -> parser.setName(v),
						() -> new RDFParseException("Metadata file does not contain name for column " + column));

		Models.getPropertyString(metadata, column, CSVW.DEFAULT).ifPresent(v -> parser.setDefaultValue(v));
		Models.getPropertyString(metadata, column, CSVW.REQUIRED)
				.ifPresent(v -> parser.setIsRequired(Boolean.parseBoolean(v)));
		Models.getPropertyString(metadata, column, CSVW.VALUE_URL).ifPresent(v -> parser.setValueURL(v));

		// use a property from a vocabulary as predicate, or create a property relative to the namespace of the CSV
		Optional<String> propertyURL = Models.getPropertyString(metadata, column, CSVW.PROPERTY_URL);
		String s = propertyURL.isPresent() ? propertyURL.get() : "_local:" + parser.getName();
		parser.setPropertyURL(metadata.getNamespaces(), s);

		return parser;
	}

	/**
	 * Get IRI of base or derived datatype
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	private IRI getDatatypeIRI(Model metadata, Resource column) {
		Optional<Value> val = Models.getProperty(metadata, column, CSVW.DATATYPE);
		if (val.isPresent()) {
			Value datatype = val.get();
			// derived datatype
			if (datatype.isBNode()) {
				val = Models.getProperty(metadata, (Resource) datatype, CSVW.BASE);
			}
		}
		if (!val.isPresent()) {
			return XSD.STRING.getIri();
		}
		Value datatype = val.get();
		if (datatype.isIRI()) {
			return (IRI) datatype;
		}
		return XSD.valueOf(datatype.stringValue().toUpperCase()).getIri();
	}

	/**
	 * Get name of the generic datatype or more specific datatype
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	private Optional<Value> getFormat(Model metadata, Resource column) {
		Optional<Value> val = Models.getProperty(metadata, column, CSVW.DATATYPE);
		if (val.isPresent()) {
			Value datatype = val.get();
			// derived datatype
			if (datatype.isBNode()) {
				Optional<Value> fmt = Models.getProperty(metadata, (Resource) datatype, CSVW.FORMAT);
				val = Models.getProperty(metadata, (Resource) fmt.get(), CSVW.BASE);
			}
		}
		return val;
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
	 * Parse a CSV file
	 *
	 * @param csvFile     URI of CSV file
	 * @param cellParsers cell parsers
	 * @param aboutURL
	 * @param aboutIndex
	 */
	private void parseCSV(Model metadata, RDFHandler handler, URI csvFile, CellParser[] cellParsers, Resource table) {
		String aboutURL = getAboutURL(metadata, table);

		// check for placeholder / column name that's being used to create subject IRI
		int aboutIndex = getAboutIndex(aboutURL, cellParsers);
		String placeholder = (aboutIndex > -1) ? cellParsers[aboutIndex].getName() : null;

		LOGGER.info("Parsing {}", csvFile);

		Charset encoding = getEncoding(metadata, table);

		long line = 0;
		try (InputStream is = csvFile.toURL().openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
				CSVReader csv = getCSVReader(metadata, table, reader)) {

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

	/**
	 * Get configured CSV file reader
	 *
	 * @param metadata
	 * @param reader
	 * @return
	 */
	private CSVReader getCSVReader(Model metadata, Resource table, Reader reader) {
		CSVParserBuilder parserBuilder = new CSVParserBuilder();
		CSVReaderBuilder builder = new CSVReaderBuilder(reader);

		Optional<Value> dialect = Models.getProperty(metadata, table, CSVW.DIALECT);
		if (dialect.isPresent()) {
			Models.getPropertyString(metadata, (Resource) dialect.get(), CSVW.DELIMITER)
					.ifPresent(v -> parserBuilder.withSeparator(v.charAt(0)));
			Models.getPropertyString(metadata, (Resource) dialect.get(), CSVW.HEADER)
					.ifPresent(v -> builder.withSkipLines(v.equalsIgnoreCase("false") ? 1 : 0));
			Models.getPropertyString(metadata, (Resource) dialect.get(), CSVW.QUOTE_CHAR)
					.ifPresent(v -> parserBuilder.withQuoteChar(v.charAt(0)));
		}

		return new CSVReaderBuilder(reader).withCSVParser(parserBuilder.build()).build();
	}

	/**
	 * Get charset of the CSV, by default this should be UTF-8
	 *
	 * @param metadata
	 * @param table
	 * @return charset
	 */
	private Charset getEncoding(Model metadata, Resource table) {
		Optional<Value> dialect = Models.getProperty(metadata, table, CSVW.DIALECT);
		if (dialect.isPresent()) {
			Optional<String> encoding = Models.getPropertyString(metadata, (Resource) dialect.get(), CSVW.ENCODING);
			if (encoding.isPresent()) {
				return Charset.forName(encoding.get());
			}
		}
		return StandardCharsets.UTF_8;
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
				return Values.iri(aboutURL.replace(placeholder, val.toString()));
			} else {
				throw new RDFParseException("NULL value in aboutURL");
			}
		} else {
			return Values.bnode();
		}
	}
}
