/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Helper class to process <a href="https://www.w3.org/TR/curie/">CURIE</a>
 * 
 * @author Bart Hanssens
 */
@InternalUseOnly
class CURIEUtil {
	public static String toString(String str) {
		int len = str.length();

		// check for "safe" CURIE"
		if (len > 3 && str.startsWith("[") && str.endsWith("]")) {
			str = str.substring(0, len - 2);
		}
		return str;
	}

	public static List<String> toStrings(String str) {
		String strs[] = str.split(" ");
		List<String> lst = new ArrayList<>(str.length());
		for (String s : strs) {
			lst.add(toString(str));
		}
		return lst;
	}
}