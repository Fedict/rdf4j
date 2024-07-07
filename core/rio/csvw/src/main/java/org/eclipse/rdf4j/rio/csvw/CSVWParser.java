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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.CSVW;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.csvw.parsers.CellParserFactory;
import org.eclipse.rdf4j.rio.csvw.parsers.Parser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;

/**
 *
 * @author Bart Hanssens
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
		Model metadata = parseMetadata(in, null, baseURI);
		System.err.println(metadata);

		Iterable<Statement> statements = metadata.getStatements(null, CSVW.TABLE_SCHEMA, null);
		for (Statement s : statements) {
			Value obj = s.getObject();
			Model cols = RDFCollections.getCollection(metadata, (Resource) obj, new LinkedHashModel());
			metadata.getStatements((Resource) obj, null, null).forEach(a -> {
				System.err.println(a);
			}
			);

			Parser p = new Parser();
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
			System.err.println(metadata);
		}

//		if (reader != null) {
//			return Rio.parse(reader, baseURI, RDFFormat.JSONLD, cfg);
//		}
		return metadata;
	}
}
