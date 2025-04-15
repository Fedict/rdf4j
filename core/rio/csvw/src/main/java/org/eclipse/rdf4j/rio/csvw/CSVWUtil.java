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

import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParser;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParserFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

/**
 * Utility class, mostly about configuring the reader based on the JSON-LD metadata
 *
 * @author Bart Hanssens
 */
public class CSVWUtil {

	/**
	 * Return URL encoded string
	 *
	 * @param s
	 * @return
	 */
	public static String encode(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		return URLEncoder.encode(s.replace(" ", "%20"), StandardCharsets.UTF_8).replace("%2520", "%20");
	}

	/**
	 * Get dialect node if present
	 *
	 * @param metadata
	 * @param table
	 * @return
	 */
	private static Resource getDialect(Model metadata, Resource table) {
		if (metadata == null) {
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
	 * Get the CSV dialect from metadata configuration
	 *
	 * @param model
	 * @param table
	 * @return map with dialect config
	 */
	protected static Map<IRI, Object> getDialectConfig(Model model, Resource table) {
		Map<IRI, Object> map = new HashMap<>();

		// for default values
		Model metadata = (model == null) ? new LinkedHashModel() : model;

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
		map.put(CSVW.ENCODING,
				Models.getPropertyString(metadata, dialect, CSVW.ENCODING).orElse("utf-8"));

		return map;
	}

	/**
	 * Get configured CSV file reader
	 *
	 * @param config
	 * @param reader
	 * @return
	 */
	protected static CSVReader getCSVReader(Map<IRI, Object> config, Reader reader) {
		CSVParserBuilder parserBuilder = new CSVParserBuilder();
		CSVReaderBuilder builder = new CSVReaderBuilder(reader);

		builder.withSkipLines((int) config.get(CSVW.SKIP_ROWS));

		parserBuilder.withSeparator(((String) config.get(CSVW.DELIMITER)).charAt(0));
		parserBuilder.withQuoteChar(((String) config.get(CSVW.QUOTE_CHAR)).charAt(0));
		parserBuilder.withEscapeChar(((String) config.get(CSVW.DOUBLE_QUOTE)).charAt(0));

		return builder.withCSVParser(parserBuilder.build()).build();
	}

	/**
	 * Get charset of the CSV, by default this should be UTF-8
	 *
	 * @param metadata
	 * @param table
	 * @return charset
	 */
	protected static Charset getEncoding(Model metadata, Resource table) {
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
	 * Get name of base or derived datatype
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	private static IRI getDatatypeIRI(Model metadata, Resource column) {
		Optional<Value> val = Models.getProperty(metadata, column, CSVW.DATATYPE);
		if (val.isPresent()) {
			Value datatype = val.get();
			// derived datatype
			if (datatype.isBNode()) {
				val = Models.getProperty(metadata, (Resource) datatype, CSVW.BASE);
			}
		}
		if (!val.isPresent()) {
			return CoreDatatype.XSD.STRING.getIri();
		}
		Value datatype = val.get();
		if (datatype.isIRI()) {
			return (IRI) datatype;
		}
		return CoreDatatype.XSD.valueOf(datatype.stringValue().toUpperCase()).getIri();
	}

	/**
	 * Get format string, e.g date format
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	private static Optional<String> getFormat(Model metadata, Resource column) {
		Optional<Value> val = Models.getProperty(metadata, column, CSVW.DATATYPE);
		if (val.isPresent() && val.get().isBNode()) {
			val = Models.getProperty(metadata, (Resource) val.get(), CSVW.FORMAT);
			if (val.isPresent() && val.get().isLiteral()) {
				return Optional.of(val.get().stringValue());
			}
		}
		return Optional.empty();
	}

	/**
	 * Get parser for specific column
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	protected static CellParser getCellParser(Model metadata, Resource column) {
		IRI datatype = getDatatypeIRI(metadata, column);

		CellParser parser = CellParserFactory.create(datatype);

		Models.getPropertyString(metadata, column, CSVW.NAME)
				.ifPresentOrElse(v -> parser.setName(v),
						() -> new RDFParseException("Metadata file does not contain name for column " + column));

		Models.getPropertyString(metadata, column, CSVW.DEFAULT).ifPresent(v -> parser.setDefaultValue(v));
		Models.getPropertyString(metadata, column, CSVW.NULL).ifPresent(v -> parser.setNullValue(v));
		Models.getPropertyString(metadata, column, CSVW.REQUIRED)
				.ifPresent(v -> parser.setRequired(Boolean.parseBoolean(v)));
		Models.getPropertyString(metadata, column, CSVW.VIRTUAL)
				.ifPresent(v -> parser.setVirtual(Boolean.parseBoolean(v)));
		Models.getPropertyString(metadata, column, CSVW.SUPPRESS_OUTPUT)
				.ifPresent(v -> parser.setVirtual(Boolean.parseBoolean(v)));

		// only useful for strings
		Models.getPropertyString(metadata, column, CSVW.LANG).ifPresent(v -> parser.setLang(v));

		// only useful for numeric
		Models.getPropertyString(metadata, column, CSVW.DECIMAL_CHAR).ifPresent(v -> parser.setDecimalChar(v));
		Models.getPropertyString(metadata, column, CSVW.GROUP_CHAR).ifPresent(v -> parser.setGroupChar(v));

		// mostly for date formats
		getFormat(metadata, column).ifPresent(v -> parser.setFormat(v));

		Models.getPropertyString(metadata, column, CSVW.TRIM)
				.ifPresent(v -> parser.setVirtual(Boolean.parseBoolean(v)));

		Models.getPropertyString(metadata, column, CSVW.ABOUT_URL).ifPresent(v -> parser.setAboutUrl(v));
		Models.getPropertyString(metadata, column, CSVW.VALUE_URL).ifPresent(v -> parser.setValueUrl(v));

		// use a property from a vocabulary as predicate, or create a property relative to the namespace of the CSV
		Optional<String> propertyURL = Models.getPropertyString(metadata, column, CSVW.PROPERTY_URL);
		String s = propertyURL.isPresent() ? propertyURL.get() : ":" + parser.getName();
		parser.setPropertyUrl(metadata.getNamespaces(), s);

		return parser;
	}
}
