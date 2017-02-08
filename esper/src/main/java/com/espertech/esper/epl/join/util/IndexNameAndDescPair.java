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
package com.espertech.esper.epl.join.util;

public class IndexNameAndDescPair {
    private final String indexName;
    private final String indexDesc;

    public IndexNameAndDescPair(String tableName, String indexDesc) {
        this.indexName = tableName;
        this.indexDesc = indexDesc;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexDesc() {
        return indexDesc;
    }
}
