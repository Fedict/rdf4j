/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.impl;

import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

/**
 * RDFa evaluation context helper class
 * 
 * @author Bart Hanssens
 */
@InternalUseOnly
class EvaluationContext {
	private IRI base;
	private Resource parentSubject;
	private Resource parentObject;
	private Map<String,IRI> iriMappings;
	private Map<String,IRI> termMappings;
	private Map<IRI,List<IRI>> listMappings;
	private IRI defaultVocabulary;
	private List<CompletableStatement> incompleteTriples;
	private String language;

	public IRI getBase() {
		return base;
	}

	public void setBase(IRI base) {
		this.base = base;
	}

	public Resource getParentSubject() {
		return parentSubject;
	}

	public void setParentSubject(Resource parentSubject) {
		this.parentSubject = parentSubject;
	}

	public Resource getParentObject() {
		return parentObject;
	}

	public void setParentObject(Resource parentObject) {
		this.parentObject = parentObject;
	}

	public Map<String,IRI> getIriMappings() {
		return iriMappings;
	}

	public void setIriMappings(Map<String,IRI> iriMappings) {
		this.iriMappings = iriMappings;
	}

	public Map<String,IRI> getTermMappings() {
		return termMappings;
	}

	public void setTermMappings(Map<String,IRI> termMappings) {
		this.termMappings = termMappings;
	}

	public Map<IRI,List<IRI>> getListMappings() {
		return listMappings;
	}

	public void setListMappings(Map<IRI,List<IRI>> listMappings) {
		this.listMappings = listMappings;
	}

	public IRI getDefaultVocabulary() {
		return defaultVocabulary;
	}

	public void setDefaultVocabulary(IRI defaultVocabulary) {
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
}
