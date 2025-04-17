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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataInputStream;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataProvider;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataUtil;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParser;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParserFactory;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

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

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.CSVW;
	}

	@Override
	public synchronized void parse(InputStream input, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {

		clear();

		rdfHandler = getRDFHandler();

		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);
		if (minimal) {
			LOGGER.info("Minimal mode set to " + minimal);
		}
		Resource rootNode = minimal ? null : generateTablegroupNode(rdfHandler);

		boolean metadataIn = getParserConfig().get(CSVWParserSettings.METADATA_INPUT_MODE);
		CSVWMetadataProvider provider = (metadataIn)
				? new CSVWMetadataInputStream(input)
				: getParserConfig().get(CSVWParserSettings.METADATA_PROVIDER);
		Model metadata = CSVWMetadataUtil.getMetadataAsModel(provider);

		if (metadataIn && metadata.isEmpty()) {
			throw new RDFParseException("CSVW metadata input mode, but no metadata found");
		}

		if (!metadata.isEmpty()) {
			List<Value> tables = CSVWMetadataUtil.getTables(metadata);
			if (tables.isEmpty()) {
				throw new RDFParseException("CSVW metadata does not contain table info");
			}

			for (Value table : tables) {
				URI csvURI = getURI(metadata, (Resource) table, baseURI);
				if (csvURI == null) {
					throw new RDFParseException("Could not find URL for CSV file");
				}
				String csvFile = csvURI.toString();
				// add dummy namespace for resolving unspecified column names / predicates relative to CSV file
				rdfHandler.handleNamespace("", csvFile + "#");
				metadata.getNamespaces().add(new SimpleNamespace("", csvFile + "#"));

				Resource tableNode = minimal ? null : generateTableNode(rdfHandler, rootNode, csvFile);

				Model extra = CSVWMetadataUtil.getExtraMetadata(metadata, (Resource) tableNode, CSVW.TABLE_SCHEMA);
				extra.forEach(s -> rdfHandler.handleStatement(s));

				Resource tableSchema = CSVWMetadataUtil.getTableSchema(metadata, (Resource) table);
				if (tableSchema != null) {
					List<Value> columns = CSVWMetadataUtil.getColumns(metadata, tableSchema);
					if (columns.isEmpty()) {
						throw new RDFParseException("Could not find column definitions in metadata");
					}

					CellParser[] cellParsers = columns.stream()
							.map(c -> CSVWUtil.getCellParser(metadata, (Resource) c))
							.collect(Collectors.toList())
							.toArray(new CellParser[columns.size()]);
					try (InputStream inCsv = csvURI.toURL().openStream()) {
						parseCSV(metadata, rdfHandler, csvFile, inCsv, cellParsers, (Resource) table, tableNode);
					}
				} else {
					LOGGER.warn("Metadata file does not contain tableSchema for {}", csvFile);
					parseCSV(metadata, rdfHandler, csvFile, input, tableNode);
				}
			}
		} else {
			LOGGER.warn("No metadata found, fallback to simple output");
			String csvFile = getParserConfig().get(CSVWParserSettings.DATA_URL);
			if (csvFile == null || csvFile.isEmpty()) {
				csvFile = baseURI;
			}
			rdfHandler.handleNamespace("", csvFile + "#");
			Resource tableNode = minimal ? null : generateTableNode(rdfHandler, rootNode, csvFile);
			parseCSV(null, rdfHandler, csvFile, input, tableNode);
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
	 * Get the location of the CSV file
	 *
	 * @param metadata
	 * @param subject
	 * @param baseURI
	 */
	private URI getURI(Model metadata, Resource table, String baseURI) {
		if (metadata == null || table == null) {
			if (baseURI != null && !baseURI.isEmpty()) {
				try {
					return new URI(baseURI);
				} catch (URISyntaxException ex) {
					LOGGER.error("BaseURI is invalid: ", ex.getMessage());
				}
			}
			return null;
		}

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
	 * Get "about" URL template, to be used to create the subject of the triples
	 *
	 * @param metadata
	 * @param subject
	 * @return aboutURL or null
	 */
	private String getAboutURL(Model metadata, Resource table) {
		String url = Models.getPropertyString(metadata, table, CSVW.ABOUT_URL)
				.orElse(Models
						.getPropertyString(metadata, CSVWMetadataUtil.getTableSchema(metadata, table), CSVW.ABOUT_URL)
						.orElse(null));
		if (url == null) {
			return null;
		}
		if (url.startsWith("#")) {
			Optional<Namespace> localNs = metadata.getNamespace("");
			if (localNs.isPresent()) {
				url = localNs.get().getName() + url.substring(1);
			}
		}
		return url;
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
	 * @param rootNode
	 * @param csvURL
	 * @return
	 */
	private Resource generateTableNode(RDFHandler handler, Resource rootNode, String csvUrl) {
		BNode node = Values.bnode();
		handler.handleStatement(Statements.statement(rootNode, CSVW.HAS_TABLE, node, null));
		handler.handleStatement(Statements.statement(node, RDF.TYPE, CSVW.TABLE, null));
		handler.handleStatement(Statements.statement(node, CSVW.URL, Values.iri(csvUrl), null));

		return node;
	}

	/**
	 * Create row statements and return row node
	 *
	 * @param handler
	 * @param tableNode
	 * @param rowSubject
	 * @param rowURL
	 * @param rownum
	 * @return
	 */
	private Resource generateRowNode(RDFHandler handler, Resource tableNode, Resource rowSubject, Resource rowURL,
			long rownum) {
		BNode rownode = Values.bnode();
		Resource node = (rowSubject != null) ? rowSubject : Values.bnode();

		handler.handleStatement(Statements.statement(tableNode, CSVW.HAS_ROW, rownode, null));
		handler.handleStatement(Statements.statement(rownode, RDF.TYPE, CSVW.ROW, null));
		handler.handleStatement(
				Statements.statement(rownode, CSVW.ROWNUM, Values.literal(String.valueOf(rownum), XSD.INTEGER), null));
		handler.handleStatement(Statements.statement(rownode, CSVW.URL, rowURL, null));
		handler.handleStatement(Statements.statement(rownode, CSVW.DESCRIBES, node, null));

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
	 * Parse a CSV file without metadata column definitions or without any metadata at all.
	 *
	 * @param handler
	 * @param baseURI
	 * @param csvFile
	 * @param tableNode
	 */
	private void parseCSV(Model metadata, RDFHandler handler, String csvFile, InputStream input,
			Resource tableNode) {
		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);

		Map<IRI, Object> dialect = CSVWMetadataUtil.getDialectConfig(metadata, tableNode);
		Charset encoding = Charset.forName((String) dialect.get(CSVW.ENCODING));

		long line = 1;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, encoding));
				CSVReader csv = CSVWUtil.getCSVReader(dialect, reader)) {

			String[] cells;
			// assume first line is header
			String[] header;

			int headerRows = 1;
			if ((boolean) dialect.get(CSVW.HEADER)) {
				header = csv.readNext();
			} else {
				headerRows = 0;
				header = csv.peek().clone(); // make sure to don't change the CSV itself
				for (int i = 0; i < header.length; i++) {
					header[i] = "_col." + (i + 1);
				}
			}

			CellParser[] cellParsers = new CellParser[header.length];
			for (int i = 0; i < header.length; i++) {
				cellParsers[i] = CellParserFactory.create(XSD.STRING.getIri());
				cellParsers[i].setName(header[i]);
				cellParsers[i].setPropertyUrl(csvFile + "#" + CSVWUtil.encode(header[i]));
			}

			while ((cells = csv.readNext()) != null) {
				// row number + 1 to compensate for header
				Resource rowURL = Values.iri(csvFile + "#row=" + (line + headerRows));
				Resource rowNode = minimal ? null : generateRowNode(rdfHandler, tableNode, null, rowURL, line);

				// csv cells
				for (int i = 0; i < cells.length; i++) {
					Value val = cellParsers[i].parse(cells[i]);
					if (val != null) {
						handler.handleStatement(
								Statements.statement(rowNode, cellParsers[i].getPropertyUrl(), val, null));
					}
				}
				line++;
			}
		} catch (IOException | CsvValidationException ex) {
			throw new RDFParseException("Error parsing", ex, line, -1);
		}
	}

	/**
	 * Parse a CSVW file
	 *
	 * @param metadata
	 * @param handler
	 * @param csvFile
	 * @param input
	 * @param cellParsers
	 * @param table
	 * @param tableNode
	 */
	private void parseCSV(Model metadata, RDFHandler handler, String csvFile, InputStream input,
			CellParser[] cellParsers, Resource table, Resource tableNode) {
		Map<IRI, Object> dialect = CSVWMetadataUtil.getDialectConfig(metadata, tableNode);
		Charset encoding = Charset.forName((String) dialect.get(CSVW.ENCODING));

		String aboutURL = getAboutURL(metadata, table);
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
		int col = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, encoding));
				CSVReader csv = CSVWUtil.getCSVReader(dialect, reader)) {

			Map<String, String> values = null;
			String[] cells;

			// assume first line is header
			String[] header = csv.readNext();

			while ((cells = csv.readNext()) != null) {
				Resource rowURL = Values.iri(csvFile + "#row=" + (line + 1));
				Resource rowSubject = getIRIorBnode(cellParsers, cells, aboutURL, aboutIndex, placeholder);
				Resource rowNode = minimal ? null : generateRowNode(rdfHandler, tableNode, rowSubject, rowURL, line);

				if (doReplace) {
					values = new HashMap<>(cells.length + 4, 1.0f);
					values.put("{_row}", Long.toString(line));
				}

				// csv cells
				for (col = 0; col < cells.length; col++) {
					if (doReplace) {
						values.put("{_col}", Long.toString(col + 1));
					}
					if (col == aboutIndex) { // already processed to get subject
						if (doReplace) {
							values.put(cellParsers[col].getNameEncoded(),
									cellParsers[col].parse(cells[col]).stringValue());
						}
						// continue;
					}
					Value val = cellParsers[col].parse(cells[col]);

					if (val != null && doReplace) {
						values.put(cellParsers[col].getNameEncoded(), val.stringValue());
					}
					if (!cellParsers[col].isSuppressed() && !needReplacement[col] && val != null) {
						handler.handleStatement(buildStatement(cellParsers[col], cells[col], rowNode, val));
					}
				}
				// second pass, this time to retrieve replace placeholders in URLs with column values
				for (col = 0; col < cells.length; col++) {
					if (col == aboutIndex || !needReplacement[col]) { // already processed to get subject
						continue;
					}
					if (!cellParsers[col].isSuppressed()) {
						handler.handleStatement(buildStatement(cellParsers[col], cells[col], rowNode, values));
					}
				}
				// virtual columns, if any
				for (col = cells.length; col < cellParsers.length; col++) {
					if (doReplace) {
						values.put("{_col}", Long.toString(col));
					}
					handler.handleStatement(buildStatement(cellParsers[col], null, rowNode, values));
				}
				line++;
			}
		} catch (IOException | CsvValidationException ex) {
			throw new RDFParseException("Error parsing ", ex, line, -1);
		} catch (IllegalArgumentException iae) {
			throw new RDFParseException("Error parsing cell value ", iae, line, col + 1);
		}
	}

	/**
	 * Generate triple statement, using the cell parser to obtain the predicate
	 *
	 * @param handler
	 * @param cellParser
	 * @param cell
	 * @param aboutSubject
	 * @param val
	 */
	private Statement buildStatement(CellParser cellParser, String cell, Resource aboutSubject, Value val) {
		Resource subj = cellParser.getAboutUrl(cell);
		if (subj == null) {
			subj = aboutSubject;
		}
		IRI pred = cellParser.getPropertyUrl();
		Value obj = cellParser.getValueUrl(cell);
		if (obj == null) {
			obj = val;
		}
		return Statements.statement(subj, pred, obj, null);
	}

	/**
	 * Generate triple statement
	 *
	 * @param handler
	 * @param cellParser
	 * @param cell
	 * @param aboutSubject
	 * @param values
	 */
	private Statement buildStatement(CellParser cellParser, String cell, Resource aboutSubject,
			Map<String, String> values) {
		Resource subj = cellParser.getAboutUrl(values);
		if (subj == null) {
			subj = aboutSubject;
		}
		IRI pred = cellParser.getPropertyUrl();
		Value obj = cellParser.getValueUrl(values, cell);
		if (obj == null && cell != null) {
			obj = cellParser.parse(cell);
		}

		return Statements.statement(subj, pred, obj, null);
	}

	/**
	 * Get subject IRI or blank node
	 *
	 * @param cellParsers
	 * @param aboutURL
	 * @param aboutIndex
	 * @param placeholder
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
