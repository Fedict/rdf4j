/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Bart.Hanssens
 */
public class RDFaUtil {

	/**
	 * Build the initial context from a JSON-LD context file
	 * 
	 * <a href="http://www.w3.org/2013/json-ld-context/rdfa11">RDFa Core Initial Context</a>
	 * 
	 * @return 
	 */
	public static Map<String,String> buildContext() {
		Map<String,String> map = new HashMap<>();

		try (InputStream is = RDFaUtil.class.getClassLoader().getResourceAsStream("rdfa-context.jsonld");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			reader.lines()
				.map(s -> s.replaceAll("[\"'\\s,]", "").split(":", 2))
				.filter(s -> s.length == 2)
				.forEach(s -> map.put(s[0], s[1]));
		} catch (IOException ioe) {
			//
		}
		return map;
	}
}
