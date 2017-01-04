/*
 *  Copyright (c) 2015-2017 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.examples.repository;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.IOException;
import java.io.InputStream;

/**
 * RDF Tutorial example 12: Adding an RDF file directly to the database
 *
 * @author Jeen Broekstra
 */
public class Example12AddRDFToDatabase {

	public static void main(String[] args)
			throws IOException
	{
		// Create a new Repository.
		Repository db = new SailRepository(new MemoryStore());
		db.initialize();

		// Open a connection to the database
		try (RepositoryConnection conn = db.getConnection()) {
			String filename = "example-data-artists.ttl";
			try (InputStream input =
					Example12AddRDFToDatabase.class.getResourceAsStream("/" + filename)) {
				// add the RDF data from the inputstream directly to our database
				conn.add(input, "", RDFFormat.TURTLE );
			}

			// let's check that our data is actually in the database
			try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
				while (result.hasNext()) {
					Statement st = result.next();
					System.out.println("db contains: " + st);
				}
			}
		}
		finally {
			// before our program exits, make sure the database is properly shut down.
			db.shutDown();
		}
	}
}
