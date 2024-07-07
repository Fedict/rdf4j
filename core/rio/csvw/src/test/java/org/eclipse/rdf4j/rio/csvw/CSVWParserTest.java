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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart.Hanssens
 */
public class CSVWParserTest {
	@Test
	public void testCSVWParser() throws IOException {
		CSVWParser parser = new CSVWParser();
		parser.getParserConfig().set(BasicWriterSettings.BASE_DIRECTIVE, true);
		parser.parse(new FileInputStream("src/test/resources/org/eclipse/rdf4j/rio/csvw/painters-metadata.json"));
	}
}
