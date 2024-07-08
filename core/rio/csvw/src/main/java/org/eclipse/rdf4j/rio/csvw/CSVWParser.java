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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
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
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.csvw.parsers.Parser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

/**
 * Basic (experimental) CSV on the Web Parser
 *
 * @author Bart Hanssens
 * @see <a href="https://w3c.github.io/csvw/primer/">CSV on the Web Primer</a>
 *
 * @since 5.1.0
 */
public class CSVWParser extends AbstractRDFParser {

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.CSVW;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {

		clear();

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
			Resource tableSchema = getTableSchema(metadata, (Resource) table);
			List<Value> columns = getColumns(metadata, tableSchema);
			Parser[] cellParsers = columns.stream()
										.map(c -> getCellParser(metadata, (Resource) c))
										.collect(Collectors.toList())
										.toArray(new Parser[columns.size()]);
			parseCSV(csvFile, cellParsers);
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
	 * Get the subject of the table(s)
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
	 * Get URL of the CSV file
	 * 
	 * @param metadata
	 * @param subject
	 * @param baseURI
	 */
	private URI getURL(Model metadata, Resource subject, String baseURI) {
		Optional<String> val = Models.getPropertyString(metadata, subject, CSVW.URL);
		if (val.isPresent()) {
			String s = val.get();
			if (s.startsWith("http")) {
				return URI.create(s);
			}
			return URI.create(baseURI).resolve(s);
		}
		return null;
	}

	/**
	 * Get tableschema for a given table
	 *
	 * @param metadata
	 * @param subject
	 * @return
	 * @throws RDFParseException
	 */
	private Resource getTableSchema(Model metadata, Resource subject) throws RDFParseException {
		return Models.getPropertyResource(metadata, subject, CSVW.TABLE_SCHEMA)
				.orElseThrow(() -> new RDFParseException("Metadata file does not contain tableSchema for " + subject));
	}

	/**
	 * Get columns for a given tableschema
	 *
	 * @param metadata
	 * @param subject
	 * @return
	 * @throws RDFParseException
	 */
	private List<Value> getColumns(Model metadata, Resource subject) throws RDFParseException {
		Optional<Resource> head = Models.getPropertyResource(metadata, subject, CSVW.COLUMN);
		if (!head.isPresent()) {
			throw new RDFParseException("Metadata file does not contain columns for " + subject);
		}
		return RDFCollections.asValues(metadata, head.get(), new ArrayList<>());
	}

	/**
	 *
	 * @param metadata
	 * @param table
	 * @return
	 */
	private Parser getCellParser(Model metadata, Resource subject) {
		Parser parser = new Parser();

		Optional<Value> name = Models.getProperty(metadata, subject, CSVW.NAME);
		if (!name.isPresent()) {
			throw new RDFParseException("Metadata file does not contain name for column " + subject);
		}
		parser.setName(name.get().stringValue());

		Optional<Value> defaultVal = Models.getProperty(metadata, subject, CSVW.DEFAULT);
		if (defaultVal.isPresent()) {
			parser.setDefaultValue(defaultVal.get().stringValue());
		}

		Optional<Value> dataType = Models.getProperty(metadata, subject, CSVW.DATATYPE);
		parser.setDataType((IRI) dataType.orElse(XSD.STRING.getIri()));

		Optional<Value> propertyURL = Models.getProperty(metadata, subject, CSVW.PROPERTY_URL);
		if (propertyURL.isPresent()) {
			parser.setPropertyURL(propertyURL.get().toString());
		}
		
		Optional<Value> valueURL = Models.getProperty(metadata, subject, CSVW.VALUE_URL);
		if (valueURL.isPresent()) {
			parser.setValueURL(valueURL.get().toString());
		}
		return parser;
	}

	private IRI getDataType(Model metadata, Value col) {
		return XSD.STRING.getIri();
	}
	
	/**
	 * Parse a CSV file
	 * 
	 * @param csvFile URI of CSV file
	 * @param cellParsers cell parsers
	 */
	private void parseCSV(URI csvFile, Parser[] cellParsers) {
		CSVParser parser = new CSVParserBuilder().build();

		try(InputStream is = csvFile.toURL().openStream();
			BufferedReader buf = new BufferedReader(new InputStreamReader(is));
			CSVReader csv = new CSVReaderBuilder(buf).withSkipLines(1).withCSVParser(parser).build()) {
			
			String[] cells;
			while ((cells = csv.readNext()) != null) {
			
				/* would it make much difference if processed in parallel ?
				final String[] c = cells;
				IntStream.range(0, cells.length)
						.parallel()
						.forEach(i -> cellParsers[i].parse(c[i]));
				*/
				for(int i = 0; i < cells.length; i++) {
					cellParsers[i].parse(cells[i]);	
				}	
			}
		} catch (IOException| CsvValidationException ex) {
			throw new RDFParseException("Error parsing " + csvFile, ex);
		}
	}
}
