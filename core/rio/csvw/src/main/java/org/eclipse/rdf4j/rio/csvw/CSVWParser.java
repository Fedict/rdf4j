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
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
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

		if (rdfHandler != null) {
			rdfHandler.startRDF();
		}

		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);
		if (minimal) {
			LOGGER.info("Minimal mode is set");
		}

		Model metadata = getMetadata(input);
		Resource tableGroup = generateTablegroup(minimal, rdfHandler);

		Resource rootSubject = CSVWMetadataUtil.getRootSubject(metadata);
		// Model comments = CSVWMetadataUtil.getComments(metadata, rootSubject, tableGroup);
//		comments.forEach(s -> rdfHandler.handleStatement(s));

		if (!metadata.isEmpty()) {
			List<Resource> tables = CSVWMetadataUtil.getTables(metadata);
			if (tables.isEmpty()) {
				throw new RDFParseException("CSVW metadata does not contain table info");
			}
			for (Resource table : tables) {
				String csvFile = getCSVFile(metadata, table, rootSubject, baseURI);
				Resource tableSubject = generateTable(input, metadata, rootSubject, tableGroup, table, baseURI,
						minimal);
				parseCSV(input, metadata, rdfHandler, csvFile, rootSubject, table, tableSubject);
			}
		} else {
			LOGGER.warn("No metadata found, fallback to simple output");
			String csvFile = getCSVFile(baseURI);

			rdfHandler.handleNamespace("", csvFile + "#");
			Resource tableSubject = minimal ? null : generateTable(rdfHandler, null, tableGroup, csvFile);
			parseCSV(null, rdfHandler, csvFile, input, tableSubject);
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
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
	 * Get CSVW metadata as RDF model
	 *
	 * @param input
	 * @return
	 */
	private Model getMetadata(InputStream input) throws IOException {
		boolean metadataIn = getParserConfig().get(CSVWParserSettings.METADATA_INPUT_MODE);
		if (metadataIn) {
			String url = getParserConfig().get(CSVWParserSettings.METADATA_URL);
			LOGGER.info("Metadata input mode, reading from {}", url);
		}
		CSVWMetadataProvider provider = (metadataIn)
				? new CSVWMetadataInputStream(input)
				: getParserConfig().get(CSVWParserSettings.METADATA_PROVIDER);
		Model metadata = CSVWMetadataUtil.getMetadataAsModel(provider);
		if (metadataIn && metadata.isEmpty()) {
			throw new RDFParseException("CSVW metadata input mode, but no metadata found");
		}
		for (Namespace ns : provider.getNamespaces()) {
			metadata.setNamespace(ns);
		}
		return metadata;
	}

	/**
	 * Generate a table
	 *
	 * @param metadata
	 * @param table
	 * @param rootSubject
	 * @param baseURI
	 */
	private Resource generateTable(InputStream input, Model metadata, Resource rootSubject, Resource tableGroup,
			Resource table, String baseURI, boolean minimal) throws IOException {
		URI csvURI = getURI(metadata, table, rootSubject, baseURI);
		if (csvURI == null) {
			throw new RDFParseException("Could not find URL for CSV file");
		}
		String csvFile = csvURI.toString();
		// add dummy namespace for resolving unspecified column names / predicates relative to CSV file
		metadata.getNamespaces().removeIf(ns -> ns.getPrefix().isEmpty());
		metadata.getNamespaces().add(new SimpleNamespace("", csvFile + "#"));

		for (Namespace ns : metadata.getNamespaces()) {
			if (!ns.getPrefix().isEmpty()) {
				rdfHandler.handleNamespace(ns.getPrefix(), ns.getName());
			}
		}

		Resource tableSubject;
		if (!minimal) {
			tableSubject = CSVWMetadataUtil.getTableSubject(table, rootSubject);
			tableSubject = generateTable(rdfHandler, tableSubject, tableGroup, csvFile);

			Model comments = CSVWMetadataUtil.getComments(metadata, table, tableSubject);
			comments.forEach(s -> rdfHandler.handleStatement(s));
		} else {
			tableSubject = null;
		}
		return tableSubject;
	}

	/**
	 * Get cell parsers based upon metadata description
	 *
	 * @param input
	 * @param metadata
	 * @param rootSubject
	 * @param tableGroup
	 * @param table
	 * @param baseURI
	 * @param minimal
	 * @return
	 * @throws IOException
	 */
	private CellParser[] getCellParsers(Model metadata, Resource rootSubject, Resource table) {
		Resource tableSchema = CSVWMetadataUtil.getTableSchema(metadata, table);
		if (tableSchema == null) {
			return null;
		}
		List<Resource> columns = CSVWMetadataUtil.getColumns(metadata, tableSchema);
		if (columns.isEmpty()) {
			throw new RDFParseException("Could not find column definitions in metadata");
		}
		CellParser[] cellParsers = new CellParser[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			cellParsers[i] = getCellParser(metadata, rootSubject, table, tableSchema, columns.get(i));
			cellParsers[i].setColumn(i + 1);
		}
		return cellParsers;
	}

	/**
	 * Get cell parsers based on CSV header
	 *
	 * @param header
	 * @param csvFile
	 * @return
	 */
	private CellParser[] getCellParsers(String[] header, String csvFile) {
		CellParser[] cellParsers = new CellParser[header.length];

		for (int i = 0; i < header.length; i++) {
			cellParsers[i] = CellParserFactory.create(XSD.STRING.getIri());
			cellParsers[i].setName(header[i]);
			cellParsers[i].setPropertyUrl(csvFile + "#" + cellParsers[i].getNameEncoded());
			cellParsers[i].setColumn(i + 1);
		}
		return cellParsers;
	}

	/**
	 * Get parser for specific column
	 *
	 * @param metadata
	 * @param root
	 * @param table
	 * @param tableSchema
	 * @param column
	 * @return
	 */
	protected static CellParser getCellParser(Model metadata, Resource root, Resource table, Resource tableSchema,
			Resource column) {
		IRI datatype = CSVWUtil.getDatatypeIRI(metadata, column);

		CellParser parser = CellParserFactory.create(datatype);
		parser.setNamespaces(metadata.getNamespaces());

		Models.getPropertyString(metadata, root, CSVW.TRIM)
				.ifPresentOrElse(v -> parser.setTrim(v), () -> parser.setTrim("true"));

		Models.getPropertyString(metadata, column, CSVW.NAME)
				.or(() -> Models.getPropertyString(metadata, column, CSVW.TITLE))
				.ifPresentOrElse(v -> parser.setName(v),
						() -> new RDFParseException("Metadata file does not contain name for column " + column));
		Models.getPropertyString(metadata, column, CSVW.VIRTUAL)
				.ifPresent(v -> parser.setVirtual(Boolean.parseBoolean(v)));
		Models.getPropertyString(metadata, column, CSVW.SUPPRESS_OUTPUT)
				.ifPresent(v -> parser.setSuppressed(Boolean.parseBoolean(v)));

		// only useful for numeric
		Models.getPropertyString(metadata, column, CSVW.DECIMAL_CHAR)
				.ifPresentOrElse(v -> parser.setDecimalChar(v), () -> parser.setDecimalChar("."));
		Models.getPropertyString(metadata, column, CSVW.GROUP_CHAR).ifPresent(v -> parser.setGroupChar(v));

		// mostly for date formats
		CSVWUtil.getFormat(metadata, column).ifPresent(v -> parser.setFormat(v));

		// check properties that can be inherited
		Resource[] levels = new Resource[] { root, table, tableSchema, column };
		for (Resource level : levels) {
			if (level == null) {
				continue;
			}
			Models.getPropertyString(metadata, level, CSVW.ABOUT_URL).ifPresent(v -> parser.setAboutUrl(v));
			Models.getPropertyString(metadata, level, CSVW.PROPERTY_URL).ifPresent(v -> parser.setPropertyUrl(v));
			Models.getPropertyString(metadata, level, CSVW.VALUE_URL).ifPresent(v -> parser.setValueUrl(v));

			Models.getPropertyString(metadata, level, CSVW.DEFAULT).ifPresent(v -> parser.setDefaultValue(v));
			Models.getPropertyString(metadata, level, CSVW.NULL).ifPresent(v -> parser.setNullValue(v));

			// only useful for strings
			Models.getPropertyString(metadata, level, CSVW.LANG).ifPresent(v -> parser.setLang(v));

			Models.getPropertyString(metadata, level, CSVW.REQUIRED)
					.ifPresent(v -> parser.setRequired(Boolean.parseBoolean(v)));

			Models.getPropertyString(metadata, level, CSVW.SEPARATOR).ifPresent(v -> parser.setSeparator(v));
		}

		if (parser.getPropertyUrl() == null) {
			parser.setPropertyUrl(":" + parser.getNameEncoded());
		}
		return parser;
	}

	/**
	 * Get the location of the CSV file
	 *
	 * @param metadata
	 * @param subject
	 * @param baseURI
	 */
	private URI getURI(Model metadata, Resource table, Resource root, String baseURI) {
		Optional<String> val = Models.getPropertyString(metadata, table, CSVW.URL);
		if (!val.isPresent()) {
			val = Models.getPropertyString(metadata, root, CSVW.URL);
		}

		if (val.isPresent()) {
			String s = val.get();
			if (s.startsWith("http:") || s.startsWith("https:")) {
				return URI.create(s);
			}
			// relative path
			String jsonURL = getParserConfig().get(CSVWParserSettings.METADATA_URL);
			if (jsonURL != null) {
				return URI.create(jsonURL).resolve(s);
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
		Optional<String> url = Models.getPropertyString(metadata, table, CSVW.ABOUT_URL);
		if (!url.isPresent()) {
			Resource tableSchema = CSVWMetadataUtil.getTableSchema(metadata, table);
			if (tableSchema != null) {
				url = Models.getPropertyString(metadata, tableSchema, CSVW.ABOUT_URL);
			}
		}
		if (!url.isPresent()) {
			return null;
		}
		String str = url.get();
		if (str.startsWith("#")) {
			Optional<Namespace> localNs = metadata.getNamespace("");
			if (localNs.isPresent()) {
				str = localNs.get().getName() + str.substring(1);
			}
		}
		return str;
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
			if (s != null && (aboutURL.contains("{#" + s + "}") || aboutURL.contains("{" + s + "}"))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Create tablegroup statement and return metaRoot tableNode
	 *
	 * @param handler
	 * @return
	 */
	private Resource generateTablegroup(boolean minimal, RDFHandler handler) {
		if (minimal) {
			return null;
		}
		Resource subject = (Resource) Values.bnode();
		handler.handleStatement(Statements.statement(subject, RDF.TYPE, CSVW.TABLE_GROUP, null));
		return subject;
	}

	/**
	 * Create table statements and return table tableNode
	 *
	 * @param handler
	 * @param groupNode
	 * @param csvURL
	 * @return
	 */
	private Resource generateTable(RDFHandler handler, Resource tableSubject, Resource groupNode, String csvUrl) {
		Resource tableNode = (tableSubject != null) ? tableSubject : Values.bnode();
		handler.handleStatement(Statements.statement(groupNode, CSVW.HAS_TABLE, tableNode, null));
		handler.handleStatement(Statements.statement(tableNode, RDF.TYPE, CSVW.TABLE, null));
		handler.handleStatement(Statements.statement(tableNode, CSVW.URL, Values.iri(csvUrl), null));

		return tableNode;
	}

	/**
	 * Generate CSVW Row
	 *
	 * @param handler
	 * @param tableSubject
	 * @param rowURL
	 * @param rownum
	 * @return
	 */
	private Resource generateRow(RDFHandler handler, Resource tableSubject, Resource rowURL, long rownum) {
		BNode rownode = Values.bnode();
		handler.handleStatement(Statements.statement(tableSubject, CSVW.HAS_ROW, rownode, null));
		handler.handleStatement(Statements.statement(rownode, RDF.TYPE, CSVW.ROW, null));
		handler.handleStatement(
				Statements.statement(rownode, CSVW.ROWNUM, Values.literal(String.valueOf(rownum), XSD.INTEGER), null));
		handler.handleStatement(Statements.statement(rownode, CSVW.URL, rowURL, null));
		return rownode;
	}

	/**
	 * Generate CSVW describes for a CSVW Row
	 *
	 * @param handler
	 * @param rowSubject
	 * @param rowURL
	 * @return
	 */
	private Resource generateDescribes(RDFHandler handler, Resource rowSubject, Resource rowID) {
		Resource node = (rowID != null) ? rowID : Values.bnode();
		// handler.handleStatement(Statements.statement(rowSubject, CSVW.DESCRIBES, node, null));
		return node;
	}

	/**
	 * Get header row
	 *
	 * @param csv
	 * @param headerRows
	 * @return
	 * @throws IOException
	 * @throws CsvValidationException
	 */
	private String[] getHeader(CSVReader csv, int headerRows) throws IOException, CsvValidationException {
		String[] header;

		if (headerRows > 0) {
			header = csv.readNext();
		} else {
			header = csv.peek().clone(); // make sure to don't change the CSV itself
			for (int i = 0; i < header.length; i++) {
				header[i] = "_col." + (i + 1);
			}
		}
		return header;
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
		Charset encoding = getCharset(dialect);

		long line = 1;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, encoding));
				CSVReader csv = CSVWUtil.getCSVReader(dialect, reader)) {

			int headerRows = (int) dialect.get(CSVW.HEADER_ROW_COUNT);
			String[] header = getHeader(csv, headerRows);

			CellParser[] cellParsers = getCellParsers(header, csvFile);

			String[] cells;
			while ((cells = csv.readNext()) != null) {
				// row number + 1 to compensate for header
				Resource rowURL = Values.iri(csvFile + "#row=" + (line + headerRows));
				Resource rowSubject = minimal ? Values.bnode() : generateRow(rdfHandler, tableNode, rowURL, line);
				Resource rowID = null;
				Resource describes = Values.bnode();
				handler.handleStatement(Statements.statement(rowSubject, CSVW.DESCRIBES, describes, null));

				// csv cells
				for (int i = 0; i < cells.length; i++) {
					Value val = cellParsers[i].parse(cells[i]);
					if (val != null) {
						handler.handleStatement(
								Statements.statement(describes, cellParsers[i].getPropertyUrl(), val, null));
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
	private void parseCSV(InputStream input, Model metadata, RDFHandler handler, String csvFile,
			Resource rootSubject, Resource table, Resource tableNode) {

		CellParser[] cellParsers = getCellParsers(metadata, rootSubject, table);
		if (cellParsers == null) {
			parseCSV(metadata, handler, csvFile, input, tableNode);
			return;
		}

		Map<IRI, Object> dialect = CSVWMetadataUtil.getDialectConfig(metadata, tableNode);
		Charset charset = getCharset(dialect);

		String aboutURL = getAboutURL(metadata, table);
		boolean minimal = getParserConfig().get(CSVWParserSettings.MINIMAL_MODE);

		// check for placeholder / column name that's being used to create subject IRI
		int aboutIndex = getAboutIndex(aboutURL, cellParsers);
		String placeholder = (aboutIndex > -1) ? cellParsers[aboutIndex].getNameEncoded() : null;

		long line = 1;
		int col = 0;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
				CSVReader csv = CSVWUtil.getCSVReader(dialect, reader)) {

			Map<String, String> replaceValues = new HashMap<>();
			String[] cells;

			int headerRows = (int) dialect.get(CSVW.HEADER_ROW_COUNT);
			getHeader(csv, headerRows);

			while ((cells = csv.readNext()) != null) {
				Resource rowURL = Values.iri(csvFile + "#row=" + (line + headerRows));
				Resource rowID = getRowAboutURL(cellParsers, cells, aboutURL, aboutIndex, line, placeholder);

				Resource rowSubject = minimal ? Values.bnode() : generateRow(rdfHandler, tableNode, rowURL, line);

				replaceValues = new HashMap<>(cells.length + 5, 1.0f);
				replaceValues.put("_row", Long.toString(line));

				Resource describes = (minimal || rowID == null) ? Values.bnode() : rowID;

				// csv cells
				for (col = 0; col < cells.length; col++) {
					String encoded = cellParsers[col].getNameEncoded();
					Value val = cellParsers[col].parse(cells[col]);
					if (val != null) {
						replaceValues.put(encoded, cellParsers[col].parse(cells[col]).stringValue());
					}
					IRI about = cellParsers[col].getAboutUrl(replaceValues);
					if (!minimal) {
						Resource node = (about != null && about.isIRI()) ? about : describes;
						handler.handleStatement(
								Statements.statement(rowSubject, CSVW.DESCRIBES, node, null));
					}
					if (!cellParsers[col].isSuppressed()) {
						generateStatements(handler, cellParsers[col], cells[col], describes, replaceValues, line, col);
					}
				}

				// virtual columns, if any
				for (col = cells.length; col < cellParsers.length; col++) {
					generateStatements(handler, cellParsers[col], (String) null, describes, replaceValues, line, col);
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
	 * Generate triple statement
	 *
	 * @param handler
	 * @param cellParser
	 * @param cell
	 * @param aboutSubject
	 * @param replaceValues
	 */
	private void generateStatements(RDFHandler handler, CellParser cellParser, String cell, Resource aboutSubject,
			Map<String, String> replaceValues, long line, int col) {
		Resource subj = cellParser.getAboutUrl(replaceValues);
		if (subj == null) {
			subj = aboutSubject;
		}
		if (subj == null) {
			throw new RDFParseException("Subject in statement is null", line, col);
		}

		IRI pred = cellParser.getPropertyUrl(replaceValues);
		if (pred == null) {
			throw new RDFParseException("Predicate in statement is null", line, col);
		}

		// one value per CSV cell, or more ?
		if (cellParser.getSeparator() == null) {
			Value obj = cellParser.getValueUrl(replaceValues);
			if (obj == null && cell != null) {
				obj = cellParser.parse(cell);
			}
			if (obj != null) {
				handler.handleStatement(Statements.statement(subj, pred, obj, null));
			}
		} else {
			Value[] objs = cellParser.parseMultiple(cell);
			if (objs != null) {
				for (Value obj : objs) {
					handler.handleStatement(Statements.statement(subj, pred, obj, null));
				}
			}
		}
	}

	/**
	 * Get subject IRI or blank tableNode
	 *
	 * @param cellParsers
	 * @param aboutURL
	 * @param aboutIndex
	 * @param placeholder
	 */
	private Resource getRowAboutURL(CellParser[] cellParsers, String[] cells, String aboutURL, int aboutIndex,
			long line, String placeholder) {

		if (aboutIndex > -1) {
			Value val = cellParsers[aboutIndex].parse(cells[aboutIndex]);
			if (val != null) {
				String s = val.stringValue();
				aboutURL = aboutURL.replace("{#" + placeholder + "}", "#" + s)
						.replace("{" + placeholder + "}", s);
				return Values.iri(aboutURL);
			} else {
				throw new RDFParseException("NULL value in aboutURL");
			}
		}

		if (aboutURL != null && aboutURL.contains("_row")) {
			aboutURL = aboutURL.replace("{#_row}", "#" + line).replace("{_row}", "" + line);
			return Values.iri(aboutURL);
		}
		return null;
	}

	/**
	 * Get character set from CSV dialect
	 *
	 * @param dialect
	 * @return character set
	 */
	private Charset getCharset(Map<IRI, Object> dialect) {
		String encoding = (String) dialect.get(CSVW.ENCODING);
		try {
			return Charset.forName(encoding);
		} catch (UnsupportedCharsetException uce) {
			LOGGER.error("Charset {} not supported, using UTF-8 instead", encoding);
			return StandardCharsets.UTF_8;
		}
	}

	/**
	 * Get the name of the CSV file, based on base URI or parser settings
	 *
	 * @return name of the CSV file
	 */
	private String getCSVFile(String baseURI) {
		String csvFile = getParserConfig().get(CSVWParserSettings.DATA_URL);
		if (csvFile == null || csvFile.isEmpty()) {
			csvFile = baseURI;
		}
		return csvFile;
	}

	/**
	 * Get name of the CSV file, based on metadata
	 *
	 * @param metadata
	 * @param table
	 * @param rootSubject
	 * @param baseURI
	 * @return name of the CSV file
	 */
	private String getCSVFile(Model metadata, Resource table, Resource rootSubject, String baseURI) {
		URI csvURI = getURI(metadata, table, rootSubject, baseURI);
		if (csvURI == null) {
			throw new RDFParseException("Could not find URL for CSV file");
		}
		return csvURI.toString();
	}
}