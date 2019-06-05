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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableUnique;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepositoryEntry;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.*;

public class TableInstanceGroupedImpl extends TableInstanceGroupedBase implements TableInstanceGrouped {

    private final Map<Object, ObjectArrayBackedEventBean> rows;

    public TableInstanceGroupedImpl(Table table, AgentInstanceContext agentInstanceContext) {
        super(table, agentInstanceContext);

        PropertyHashedEventTableUnique eventTable = (PropertyHashedEventTableUnique) table.getPrimaryIndexFactory().makeEventTables(agentInstanceContext, null)[0];
        rows = (Map<Object, ObjectArrayBackedEventBean>) (Map) eventTable.getPropertyIndex();
        indexRepository.addIndex(table.getMetaData().getKeyIndexMultiKey(), new EventTableIndexRepositoryEntry(table.getMetaData().getTableName(), table.getMetaData().getTableModuleName(), eventTable));
    }

    public long size() {
        return rows.size();
    }

    public ObjectArrayBackedEventBean getRowForGroupKey(Object groupKey) {
        return rows.get(groupKey);
    }

    public Collection<EventBean> getEventCollection() {
        return (Collection<EventBean>) (Collection<?>) rows.values();
    }

    public Iterable<EventBean> getIterableTableScan() {
        return new PrimaryIndexIterable(rows);
    }

    public void deleteEvent(EventBean matchingEvent) {
        agentInstanceContext.getInstrumentationProvider().qTableDeleteEvent(matchingEvent);
        for (EventTable table : indexRepository.getTables()) {
            table.remove(matchingEvent, agentInstanceContext);
        }
        agentInstanceContext.getInstrumentationProvider().aTableDeleteEvent();
    }

    public void addExplicitIndex(String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient) throws ExprValidationException {
        indexRepository.validateAddExplicitIndex(indexName, indexModuleName, explicitIndexDesc, table.getMetaData().getInternalEventType(), new PrimaryIndexIterable(rows), getAgentInstanceContext(), isRecoveringResilient, null);
    }

    public void removeExplicitIndex(String indexName) {
        indexRepository.removeExplicitIndex(indexName);
    }

    public EventTable getIndex(String indexName) {
        if (indexName.equals(table.getMetaData().getTableName())) {
            return indexRepository.getIndexByDesc(table.getMetaData().getKeyIndexMultiKey());
        }
        return indexRepository.getExplicitIndexByName(indexName);
    }

    public void clearInstance() {
        for (EventTable table : indexRepository.getTables()) {
            table.destroy();
        }
    }

    public void destroy() {
        clearInstance();
    }

    public ObjectArrayBackedEventBean getCreateRowIntoTable(Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = rows.get(groupByKey);
        if (bean != null) {
            return bean;
        }
        return createRowIntoTable(groupByKey);
    }

    public Collection<Object> getGroupKeysMayMultiKey() {
        return rows.keySet();
    }

    public Collection<Object> getGroupKeys() {
        Class[] keyTypes = table.getMetaData().getKeyTypes();
        if (keyTypes.length == 1 && !keyTypes[0].isArray()) {
            return rows.keySet();
        }
        List<Object> keys = new ArrayList<>(rows.size());
        if (keyTypes.length == 1) {
            int col = table.getMetaData().getKeyColNums()[0];
            for (ObjectArrayBackedEventBean bean : rows.values()) {
                keys.add(bean.getProperties()[col]);
            }
        } else {
            int[] cols = table.getMetaData().getKeyColNums();
            for (ObjectArrayBackedEventBean bean : rows.values()) {
                Object[] mk = new Object[cols.length];
                for (int i = 0; i < cols.length; i++) {
                    mk[i] = bean.getProperties()[cols[i]];
                }
                keys.add(mk);
            }
        }
        return keys;
    }

    public void handleRowUpdated(ObjectArrayBackedEventBean updatedEvent) {
        if (agentInstanceContext.getInstrumentationProvider().activated()) {
            agentInstanceContext.getInstrumentationProvider().qTableUpdatedEvent(updatedEvent);
            agentInstanceContext.getInstrumentationProvider().aTableUpdatedEvent();
        }
        // no action
    }

    public void handleRowUpdateKeyBeforeUpdate(ObjectArrayBackedEventBean updatedEvent) {
        agentInstanceContext.getInstrumentationProvider().qaTableUpdatedEventWKeyBefore(updatedEvent);
    }

    public void handleRowUpdateKeyAfterUpdate(ObjectArrayBackedEventBean updatedEvent) {
        agentInstanceContext.getInstrumentationProvider().qaTableUpdatedEventWKeyAfter(updatedEvent);
    }

    private static class PrimaryIndexIterable implements Iterable<EventBean> {

        private final Map<Object, ObjectArrayBackedEventBean> rows;

        private PrimaryIndexIterable(Map<Object, ObjectArrayBackedEventBean> rows) {
            this.rows = rows;
        }

        public Iterator<EventBean> iterator() {
            return (Iterator<EventBean>) (Iterator<?>) rows.values().iterator();
        }
    }
}
