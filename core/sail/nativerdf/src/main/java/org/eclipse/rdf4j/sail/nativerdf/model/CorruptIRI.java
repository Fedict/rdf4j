/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptIRI is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled (see
 * ValueStore#softFailOnCorruptData).
 *
 * @author Håvard M. Ottestad
 */
public class CorruptIRI extends CorruptValue implements IRI {

	private static final long serialVersionUID = -6995615243794525852L;

	public CorruptIRI(ValueStoreRevision revision, int internalID, byte[] data) {
		super(revision, internalID, data);
	}

	public String stringValue() {
		return "CorruptIRI_with_ID_" + getInternalID();
	}

	@Override
	public String getNamespace() {
		return "CORRUPT";
	}

	@Override
	public String getLocalName() {
		return "CORRUPT";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptIRI && getInternalID() != NativeValue.UNKNOWN_ID) {
			CorruptIRI otherCorruptValue = (CorruptIRI) o;

			if (otherCorruptValue.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptValue.getValueStoreRevision())) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptValue.getInternalID();
			}
		}

		return super.equals(o);
	}

}
