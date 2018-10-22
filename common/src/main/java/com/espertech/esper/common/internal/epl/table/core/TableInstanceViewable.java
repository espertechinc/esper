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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

public class TableInstanceViewable extends ViewSupport {

    private final Table tableMetadata;
    private final TableInstance tableStateInstance;

    public TableInstanceViewable(Table tableMetadata, TableInstance tableStateInstance) {
        this.tableMetadata = tableMetadata;
        this.tableStateInstance = tableStateInstance;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // no action required
    }

    public EventType getEventType() {
        return tableMetadata.getMetaData().getPublicEventType();
    }

    public Iterator<EventBean> iterator() {
        return new TableToPublicIterator(tableStateInstance);
    }

    private static class TableToPublicIterator implements Iterator<EventBean> {
        private final TableMetadataInternalEventToPublic eventToPublic;
        private final Iterator<EventBean> iterator;
        private final TableInstance tableStateInstance;

        private TableToPublicIterator(TableInstance tableStateInstance) {
            this.eventToPublic = tableStateInstance.getTable().getEventToPublic();
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
