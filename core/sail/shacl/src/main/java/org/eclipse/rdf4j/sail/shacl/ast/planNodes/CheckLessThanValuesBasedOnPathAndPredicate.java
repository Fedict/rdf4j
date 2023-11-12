/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;

/**
 * Used by sh:equals to return any targets and values where the target has values by path that are not values by the
 * predicate, or vice versa. It returns the targets and any symmetricDifference values when comparing the set of values
 * by path and by predicate.
 *
 * @author Håvard Ottestad
 */
public class CheckLessThanValuesBasedOnPathAndPredicate extends AbstractPairwisePlanNode {

	private final ValueComparator valueComparator = new ValueComparator();

	public CheckLessThanValuesBasedOnPathAndPredicate(SailConnection connection, Resource[] dataGraph, PlanNode parent,
			IRI predicate, StatementMatcher.Variable<Resource> subject, StatementMatcher.Variable<Value> object,
			SparqlFragment targetQueryFragment, Shape shape, ConstraintComponent constraintComponent) {
		super(connection, dataGraph, parent, predicate, subject, object, targetQueryFragment, shape,
				constraintComponent);
	}

	Set<Value> getInvalidValues(Set<Value> valuesByPath, Set<Value> valuesByPredicate) {
		HashSet<Value> ret = new HashSet<>();

		for (Value value : valuesByPath) {
			for (Value value1 : valuesByPredicate) {
				int compare = valueComparator.compare(value1, value);
				if (compare >= 0) {
					ret.add(value);
				}

			}
		}

		return ret;

	}

}
