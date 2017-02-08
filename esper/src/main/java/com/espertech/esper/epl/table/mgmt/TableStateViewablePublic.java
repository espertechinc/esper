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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

public class TableStateViewablePublic extends ViewSupport {

    private final TableMetadata tableMetadata;
    private final TableStateInstance tableStateInstance;

    public TableStateViewablePublic(TableMetadata tableMetadata, TableStateInstance tableStateInstance) {
        this.tableMetadata = tableMetadata;
        this.tableStateInstance = tableStateInstance;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // no action required
    }

    public EventType getEventType() {
        return tableMetadata.getPublicEventType();
    }

    public Iterator<EventBean> iterator() {
        return new TableToPublicIterator(tableStateInstance);
    }

    private static class TableToPublicIterator implements Iterator<EventBean> {
        private final TableMetadataInternalEventToPublic eventToPublic;
        private final Iterator<EventBean> iterator;
        private final TableStateInstance tableStateInstance;

        private TableToPublicIterator(TableStateInstance tableStateInstance) {
            this.eventToPublic = tableStateInstance.getTableMetadata().getEventToPublic();
            this.iterator = tableStateInstance.getEventCollection().iterator();
            this.tableStateInstance = tableStateInstance;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public EventBean next() {
            EventBean event = iterator.next();
            return eventToPublic.convert(event, null, true, tableStateInstance.getAgentInstanceContext());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
