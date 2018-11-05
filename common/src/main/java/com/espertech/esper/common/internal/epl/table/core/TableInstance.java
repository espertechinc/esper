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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepository;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public interface TableInstance {
    Collection<EventBean> getEventCollection();

    long size();

    void addEventUnadorned(EventBean event);

    void addEvent(EventBean event);

    Table getTable();

    AgentInstanceContext getAgentInstanceContext();

    void clearInstance();

    void destroy();

    EventTableIndexRepository getIndexRepository();

    Iterable<EventBean> getIterableTableScan();

    void handleRowUpdated(ObjectArrayBackedEventBean updatedEvent);

    void deleteEvent(EventBean matchingEvent);

    void addExplicitIndex(String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient) throws ExprValidationException;

    void removeExplicitIndex(String indexName);

    EventTable getIndex(String indexName);

    ReadWriteLock getTableLevelRWLock();

    void handleRowUpdateKeyBeforeUpdate(ObjectArrayBackedEventBean updatedEvent);

    void handleRowUpdateKeyAfterUpdate(ObjectArrayBackedEventBean updatedEvent);
}
