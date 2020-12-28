/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfa.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Helper class for RDFa "incomplete statements"
 * 
 * @author Bart Hanssens
 */
public class CompletableStatement implements Statement {
	private Resource subject;
	private IRI predicate;
	private Value object;

	@Override
	public Resource getContext() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Resource getSubject() {
		return subject;
	}

	/**
	 * Set the suject of the statement
	 * 
	 * @param subject new subject
	 */
	public void setSubject(Resource subject) {
		this.subject = subject;
	}

	/**
	 * Only set the subject of the statement if it has not been set before
	 * 
	 * @param subject new subject
	 */
	public void setSubjectIfNotSet(Resource subject) {
		if (this.subject == null) {
			this.subject = subject;
		}
	}

	@Override
	public IRI getPredicate() {
		return predicate;
	}

	/**
	 * Set the predicate of the statement
	 * 
	 * @param predicate new predicate
	 */
	public void setPredicate(IRI predicate) {
		this.predicate = predicate;
	}

	/**
	 * Only set the predicate of the statement if it has not been set before
	 * 
	 * @param predicate new predicate
	 */
	public void setPredicateIfNotSet(IRI predicate) {
		if (this.predicate == null) {
			this.predicate = predicate;
		}
	}

	@Override
	public Value getObject() {
		return object;
	}

	/**
	 * Set the object of the statement
	 * 
	 * @param object new object
	 */
	public void setObject(Value object) {
		this.object = object;
	}
	/**
	 * Only set the object of the statement if it has not been set before
	 * 
	 * @param object new object
	 */
	public void setObjectIfNotSet(Value object) {
		if (this.object == null) {
			this.object = object;
		}
	}

	/**
	 * Check if subject, predicate and object have been set
	 * 
	 * @return true if subject, predicate and object are all not null
	 */
	public boolean isComplete() {
		return (subject != null) && (predicate != null) && (object != null); 
	}
}
