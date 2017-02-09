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

public class EventTableAndNamePair {
    private final EventTable eventTable;
    private final String indexName;

    public EventTableAndNamePair(EventTable eventTable, String indexName) {
        this.eventTable = eventTable;
        this.indexName = indexName;
    }

    public EventTable getEventTable() {
        return eventTable;
    }

    public String getIndexName() {
        return indexName;
    }
}
