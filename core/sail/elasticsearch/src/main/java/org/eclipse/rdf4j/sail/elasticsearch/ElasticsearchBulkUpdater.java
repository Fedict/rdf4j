/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;

public class ElasticsearchBulkUpdater implements BulkUpdater {

	private final ElasticsearchClient client;

	private final BulkRequest.Builder bulkRequest;

	public ElasticsearchBulkUpdater(ElasticsearchClient client) {
		this.client = client;
		this.bulkRequest = new BulkRequest.Builder();
	}

	@Override
	public void add(SearchDocument doc) {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.operations(op -> 
			op.create(add -> add.index(esDoc.getIndex())
								.id(esDoc.getId())
								.document(esDoc.getSource())
			));
	}

	@Override
	public void update(SearchDocument doc) {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.operations(op ->
			op.update(upd -> upd.index(esDoc.getIndex())
								.id(esDoc.getId())
								.ifSeqNo(esDoc.getSeqNo())
								.ifPrimaryTerm(esDoc.getPrimaryTerm())
								.action(act -> act.doc(esDoc.getSource()))
			));
	}

	@Override
	public void delete(SearchDocument doc) {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.operations(op -> 
			op.delete(del -> del.index(esDoc.getIndex())
								.id(esDoc.getId())
								.ifSeqNo(esDoc.getSeqNo())
								.ifPrimaryTerm(esDoc.getPrimaryTerm())
			));
	}

	@Override
	public void end() throws IOException {
		BulkRequest req = bulkRequest.build();
		if (!req.operations().isEmpty()) {
			BulkResponse response = client.bulk(req);
			if (response.errors()) {
				String str = response.items().stream()
					.map(BulkResponseItem::error)
					.filter(Objects::nonNull)
					.map(ErrorCause::stackTrace)
					.collect(Collectors.joining("\n"));
				throw new IOException(str);
			}
		}
	}
}
