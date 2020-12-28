/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for RDFa parsers.
 * 
 * @author Bart Hanssens
 * @since 3.6.0
 */
public class RDFaParserFactory implements RDFParserFactory {

	@Override
	public RDFParser getParser() {
		return new RDFaParser();
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFA;
	}
}