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
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Models;
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
		// parserBuilder.withEscapeChar(((String) config.get(CSVW.DOUBLE_QUOTE)).charAt(0));

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
	 * @param root
	 * @param table
	 * @param tableSchema
	 * @param column
	 * @return
	 */
	protected static CellParser getCellParser(Model metadata, Resource root, Resource table, Resource tableSchema,
			Resource column) {
		IRI datatype = getDatatypeIRI(metadata, column);

		CellParser parser = CellParserFactory.create(datatype);
		parser.setNamespaces(metadata.getNamespaces());

		Models.getPropertyString(metadata, column, CSVW.NAME)
				.ifPresentOrElse(v -> parser.setName(v),
						() -> Models.getPropertyString(metadata, column, CSVW.TITLE)
								.ifPresentOrElse(t -> parser.setName(t),
										() -> new RDFParseException(
												"Metadata file does not contain name for column " + column)));
		Models.getPropertyString(metadata, column, CSVW.VIRTUAL)
				.ifPresent(v -> parser.setVirtual(Boolean.parseBoolean(v)));
		Models.getPropertyString(metadata, column, CSVW.SUPPRESS_OUTPUT)
				.ifPresent(v -> parser.setSuppressed(Boolean.parseBoolean(v)));

		// only useful for numeric
		Models.getPropertyString(metadata, column, CSVW.DECIMAL_CHAR).ifPresent(v -> parser.setDecimalChar(v));
		Models.getPropertyString(metadata, column, CSVW.GROUP_CHAR).ifPresent(v -> parser.setGroupChar(v));

		// mostly for date formats
		getFormat(metadata, column).ifPresent(v -> parser.setFormat(v));

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
}
