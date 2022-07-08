package com.tyss.optimize.common.util.tree;

import org.bson.Document;

import java.util.Comparator;
import java.util.Objects;

public class SortByExecutionOrder implements Comparator<Document> {

    @Override
    public int compare(Document doc1, Document doc2) {

        double doc1ExecutionOrder = Objects.nonNull(doc1.get("executionOrder"))? (double) doc1.get("executionOrder") : 0.0;
        double doc2ExecutionOrder = Objects.nonNull(doc2.get("executionOrder"))? (double) doc2.get("executionOrder") : 0.0;

        if( doc1ExecutionOrder > doc2ExecutionOrder) {
            return 1;
        }
        else if( doc1ExecutionOrder < doc2ExecutionOrder) {
            return -1;
        }
        else {
            return 0;
        }
    }

}
