/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.lookup;

import java.util.Arrays;
import java.util.List;

public class EventTableCreateIndexDesc {
    private final String indexName;
    private final List<IndexedPropDesc> hashProps;
    private final List<IndexedPropDesc> btreeProps;
    private final boolean unique;

    public EventTableCreateIndexDesc(String indexName, List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps, boolean unique) {
        this.indexName = indexName;
        this.hashProps = hashProps;
        this.btreeProps = btreeProps;
        this.unique = unique;
    }

    public EventTableCreateIndexDesc(String indexName, IndexMultiKey indexMultiKey) {
        this.indexName = indexName;
        this.hashProps = Arrays.asList(indexMultiKey.getHashIndexedProps());
        this.btreeProps = Arrays.asList(indexMultiKey.getRangeIndexedProps());
        this.unique = indexMultiKey.isUnique();
    }

    public String getIndexName() {
        return indexName;
    }

    public List<IndexedPropDesc> getHashProps() {
        return hashProps;
    }

    public List<IndexedPropDesc> getBtreeProps() {
        return btreeProps;
    }

    public boolean isUnique() {
        return unique;
    }
}
