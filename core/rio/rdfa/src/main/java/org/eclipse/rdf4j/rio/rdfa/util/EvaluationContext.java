/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;

/**
 * RDFa evaluation context helper class
 * 
 * @author Bart Hanssens
 */
@InternalUseOnly
public class EvaluationContext {
	private String base;
	private IRI baseIRI;
	private Resource subject;
	private Resource object;
	private Resource typedResource;
	private Map<String, String> iriMappings;
	private Map<String, String> termMappings;
	private Map<IRI, List<IRI>> listMappings;
	private String defaultVocabulary;
	private String language;
	private boolean skipElement;
	private List<CompletableStatement> incompleteTriples;

	public String getBase() {
		return base;
	}

	public IRI getBaseIRI() {
		return baseIRI;
	}

	public void setBase(String base) {
		this.base = base;
		this.baseIRI = (base != null) ? Values.iri(base) : null;
	}

	public Resource getSubject() {
		return subject;
	}

	public void setSubject(Resource subject) {
		this.subject = subject;
	}

	public Resource getObject() {
		return object;
	}

	public void setObject(Resource object) {
		this.object = object;
	}

	public Resource getTypedResource() {
		return typedResource;
	}

	public void setTypedResource(Resource typedResource) {
		this.typedResource = typedResource;
	}

	public Map<String, String> getIriMappings() {
		return iriMappings;
	}

	public void setIriMappings(Map<String, String> iriMappings) {
		this.iriMappings = iriMappings;
	}

	public Map<String, String> getTermMappings() {
		return termMappings;
	}

	public void setTermMappings(Map<String, String> termMappings) {
		this.termMappings = termMappings;
	}

	public Map<IRI, List<IRI>> getListMappings() {
		return listMappings;
	}

	public void setListMappings(Map<IRI, List<IRI>> listMappings) {
		this.listMappings = listMappings;
	}

	public String getDefaultVocabulary() {
		return defaultVocabulary;
	}

	public void setDefaultVocabulary(String defaultVocabulary) {
		this.defaultVocabulary = defaultVocabulary;
	}

	public List<CompletableStatement> getIncompleteTriples() {
		return incompleteTriples;
	}

	public void setIncompleteTriples(List<CompletableStatement> incompleteTriples) {
		this.incompleteTriples = incompleteTriples;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isSkipElement() {
		return skipElement;
	}

	public void setSkipElement(boolean skipElement) {
		this.skipElement = skipElement;
	}
}
