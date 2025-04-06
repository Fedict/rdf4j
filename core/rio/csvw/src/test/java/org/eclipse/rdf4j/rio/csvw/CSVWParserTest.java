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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.junit.jupiter.MockServerExtension;

/**
 *
 * @author Bart.Hanssens
 */
@ExtendWith(MockServerExtension.class)
public class CSVWParserTest extends AbstractTest {

	@Test
	public void testCSVWParser() throws IOException {
		Model model = new LinkedHashModel();
		CSVWParser parser = new CSVWParser();
		parser.setRDFHandler(new StatementCollector(model));
		parser.getParserConfig().set(BasicWriterSettings.BASE_DIRECTIVE, true);
		parser.parse(new FileInputStream("src/test/resources/painters-metadata.json"), getBase() + "/downloads/");

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, os);
		writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
		writer.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN);
		writer.getWriterConfig().set(JSONLDSettings.OPTIMIZE, true);
		Rio.write(model, writer);
	}
}
