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

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataFinder;
import org.eclipse.rdf4j.rio.csvw.metadata.CSVWMetadataProvider;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.eclipse.rdf4j.rio.helpers.ClassRioSetting;
import org.eclipse.rdf4j.rio.helpers.StringRioSetting;

/**
 * ParserSettings for the CSV on the Web parser features.
 * <p>
 * Several of these settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Bart Hanssens
 *
 * @since 5.1.0
 */
public class CSVWParserSettings {

	/**
	 * Boolean setting for parser to determine whether 'minimal mode' is to be used. I.e. only produce triples from the
	 * data cells, without adding table metadata .
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.csvw.minimal_mode}
	 */
	public static final BooleanRioSetting MINIMAL_MODE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.csvw.minimal_mode", "CSVWeb minimal mode", Boolean.FALSE);

	/**
	 * Boolean setting for parser to select JSON metadata file as input instead of CSV data file
	 *
	 * This implies that the parser's InputStream or Reader parameter points to a JSON-LD metadata file, not to a CSV
	 * data file
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.csvw.metadata_input_mode}
	 */
	public static final BooleanRioSetting METADATA_INPUT_MODE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.csvw.metadata_input_mode", "Input is JSON metadata instead of CDV data", true);

	/**
	 * String setting for parser to provide location of a JSON metadata file.
	 *
	 * This implies that the parser's InputStream or Reader parameter points to a CSV file, not to a JSON-LD metadata
	 * file
	 * <p>
	 * Defaults to null.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.csvw.metadata_json_file}
	 */
	public static final StringRioSetting METADATA_URI = new StringRioSetting(
			"org.eclipse.rdf4j.rio.csvw.metadata_uri", "Location of JSON metadata file", null);

	/**
	 * Class setting for parser to provide a metadata provider
	 *
	 * This implies that the parser's InputStream or Reader parameter points to a CSV file, not to a JSON-LD metadata
	 * file
	 * <p>
	 * Defaults to CSVWMetadataFinder.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.csvw.metadata_provider}
	 */
	public static final ClassRioSetting<CSVWMetadataProvider> METADATA_PROVIDER = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.csvw.metadata_provider", "Metadata provider", new CSVWMetadataFinder());

	/**
	 * String setting for parser to provide location of the CSV data file.
	 *
	 * <p>
	 * Defaults to empty.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.csvw.metadata_json_file}
	 */
	public static final StringRioSetting DATA_URL = new StringRioSetting(
			"org.eclipse.rdf4j.rio.csvw.data_url", "Location (URL) of the CSV data", "");

	/**
	 * Private constructor
	 */
	private CSVWParserSettings() {
	}
}
