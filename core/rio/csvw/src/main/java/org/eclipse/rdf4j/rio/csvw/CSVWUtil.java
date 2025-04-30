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
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.languages.BCP47LanguageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

/**
 * Utility class, mostly about configuring the reader based on the JSON-LD metadata
 *
 * @author Bart Hanssens
 */
public class CSVWUtil {
	private static final BCP47LanguageHandler LANG = new BCP47LanguageHandler();

	private static final Logger LOGGER = LoggerFactory.getLogger(CSVWUtil.class);

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
	 * Get the IRI of the datatype, can be a base or derived datatype
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	protected static IRI getDatatypeIRI(Model metadata, Resource column) {
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
		Value tmp = val.get();
		if (tmp.isIRI()) {
			return (IRI) tmp;
		}

		IRI datatype = null;
		// CSVW built-in datatypes
		String s = tmp.stringValue();
		switch (s) {
		case "datetime":
			datatype = CoreDatatype.XSD.DATETIME.getIri();
			break;
		case "number":
			datatype = CoreDatatype.XSD.DOUBLE.getIri();
			break;
		case "binary":
			datatype = CoreDatatype.XSD.BASE64BINARY.getIri();
			break;
		case "any":
			datatype = CoreDatatype.XSD.ANYURI.getIri();
			break;
		case "xml":
			datatype = CoreDatatype.RDF.XMLLITERAL.getIri();
			break;
		case "html":
			datatype = CoreDatatype.RDF.HTML.getIri();
			break;
		}
		// try XSD datatype
		if (datatype == null) {
			s = s.toUpperCase(Locale.ENGLISH)
					.replace("UNSIGNED", "UNSIGNED_")
					.replace("NONNEGATIVE", "NON_NEGATIVE")
					.replace("NEGATIVE", "NEGATIVE_")
					.replace("NONPOSITIVE", "NON_POSITIVE")
					.replace("POSITIVE", "POSITIVE_");
			try {
				datatype = CoreDatatype.XSD.valueOf(s).getIri();
			} catch (IllegalArgumentException iae) {
				LOGGER.warn("Unknown datatype {}", tmp.stringValue());
			}
		}
		return (datatype != null) ? datatype : CoreDatatype.XSD.STRING.getIri();
	}

	/**
	 * Get format string, e.g date format
	 *
	 * @param metadata
	 * @param column
	 * @return
	 */
	protected static Optional<String> getFormat(Model metadata, Resource column) {
		Optional<Value> val = Models.getProperty(metadata, column, CSVW.DATATYPE);
		if (val.isPresent() && val.get().isBNode()) {
			val = Models.getProperty(metadata, (Resource) val.get(), CSVW.FORMAT);
			if (val.isPresent() && val.get().isLiteral()) {
				return Optional.of(val.get().stringValue());
			}
		}
		return Optional.empty();
	}

	protected static Optional<String> getDatatype(Model metadata, Resource subject, IRI predicate, IRI datatype) {
		Optional<Literal> lit = Models.getPropertyLiteral(metadata, subject, predicate);
		if (!lit.isPresent()) {
			return Optional.empty();
		}
		if (!lit.get().getDatatype().equals(datatype)) {
			LOGGER.warn("Invalid data type for value {} (predicate {})", lit.get(), predicate);
			return Optional.empty();
		}
		return lit.map(l -> l.stringValue());
	}

	protected static Optional<String> getString(Model metadata, Resource subject, IRI predicate) {
		return getDatatype(metadata, subject, predicate, XSD.STRING);
	}

	protected static Optional<String> getTemplate(Model metadata, Resource subject, IRI predicate) {
		return getDatatype(metadata, subject, predicate, CSVW.URI_TEMPLATE);
	}

	protected static Optional<String> getLanguage(Model metadata, Resource subject, IRI predicate) {
		Optional<String> str = getString(metadata, subject, predicate);
		return (str.isPresent() && LANG.isRecognizedLanguage(str.get())) ? str : Optional.empty();
	}

}
