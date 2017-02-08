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
package com.espertech.esper.epl.join.table;

public class EventTableOrganization {
    private final String indexName;
    private final boolean unique;
    private final boolean coercing;
    private final int streamNum;
    private final String[] expressions;
    private final EventTableOrganizationType type;

    public EventTableOrganization(String indexName, boolean unique, boolean coercing, int streamNum, String[] expressions, EventTableOrganizationType type) {
        this.indexName = indexName;
        this.unique = unique;
        this.coercing = coercing;
        this.streamNum = streamNum;
        this.expressions = expressions;
        this.type = type;
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isUnique() {
        return unique;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public String[] getExpressions() {
        return expressions;
    }

    public EventTableOrganizationType getType() {
        return type;
    }

    public boolean isCoercing() {
        return coercing;
    }

}
