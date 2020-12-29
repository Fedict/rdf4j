/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Bart Hanssens
 */
public class CURIEUtilTest {
	@Test
	public void testCURIE() {
		String str = "ex:John";
		String res = CURIEUtil.toString(str);
		assertEquals("ex:John", res);
	}

	@Test
	public void testCURIEs() {
		String str = "ex:John ex:Doe";
		List<String> res = CURIEUtil.toStrings(str);
		assertEquals(Arrays.asList("ex:John", "ex:Doe"), res);
	}

	@Test
	public void testSafeCURIE() {
		String str = "[ex:John]";
		String res = CURIEUtil.toString(str);
		assertEquals("ex:John", res);
	}

	@Test
	public void testSafeCURIEs() {
		String str = "[ex:John] [ex:Doe]";
		List<String> res = CURIEUtil.toStrings(str);
		assertEquals(Arrays.asList("ex:John", "ex:Doe"), res);
	}
}
