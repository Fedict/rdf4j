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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
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

		Iterable<Statement> tables = metadata.getStatements(null, CSVW.TABLE_SCHEMA, null);
		for (Statement table : tables) {
			getCellParsers(metadata, table.getObject());
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
	 *
	 * @param metadata
	 * @param table
	 * @return
	 */
	private List<Parser> getCellParsers(Model metadata, Value table) {
		List<Parser> parsers = new ArrayList<>();

		Iterable<Statement> columns = metadata.getStatements((Resource) table, CSVW.COLUMNS, null);
		Statement s = columns.iterator().next();

		// the columns must be retrieved in the exact same order as they appear in the JSON metadata file,
		// especially when the CSV does not have a header row
		if (s != null) {
			List<Value> cols = RDFCollections.asValues(metadata, (Resource) s.getObject(), new ArrayList());
			for (Value col : cols) {
				Parser p = new Parser();
				p.setDataType(getDataType(metadata, col));

			}
		}
		return parsers;
	}

	private IRI getDataType(Model metadata, Value col) {
		return XSD.STRING.getIri();
	}
}
