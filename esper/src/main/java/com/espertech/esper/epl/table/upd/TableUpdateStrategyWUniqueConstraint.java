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
package com.espertech.esper.epl.table.upd;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventBean;

import java.util.Collection;
import java.util.Set;

public class TableUpdateStrategyWUniqueConstraint implements TableUpdateStrategy {

    private final EventBeanUpdateHelper updateHelper;
    private final Set<String> affectedIndexNames;

    public TableUpdateStrategyWUniqueConstraint(EventBeanUpdateHelper updateHelper, Set<String> affectedIndexNames) {
        this.updateHelper = updateHelper;
        this.affectedIndexNames = affectedIndexNames;
    }

    public void updateTable(Collection<EventBean> eventsUnsafeIter, TableStateInstance instance, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // copy references to array - as it is allowed to pass an index-originating collection
        // and those same indexes are being changed now
        EventBean[] events = new EventBean[eventsUnsafeIter.size()];
        int count = 0;
        for (EventBean event : eventsUnsafeIter) {
            events[count++] = event;
        }

        // remove from affected indexes
        for (String affectedIndexName : affectedIndexNames) {
            EventTable index = instance.getIndex(affectedIndexName);
            index.remove(events, exprEvaluatorContext);
        }

        // copy event data, since we are updating unique keys and must guarantee rollback (no half update)
        Object[][] previousData = new Object[events.length][];

        // copy and then update
        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];

            // copy non-aggregated value references
            ObjectArrayBackedEventBean updatedEvent = (ObjectArrayBackedEventBean) events[i];
            Object[] prev = new Object[updatedEvent.getProperties().length];
            System.arraycopy(updatedEvent.getProperties(), 0, prev, 0, prev.length);
            previousData[i] = prev;

            // if "initial.property" is part of the assignment expressions, provide initial value event
            if (updateHelper.isRequiresStream2InitialValueEvent()) {
                eventsPerStream[2] = new ObjectArrayEventBean(prev, updatedEvent.getEventType());
            }

            // apply in-place updates
            instance.handleRowUpdateKeyBeforeUpdate(updatedEvent);
            updateHelper.updateNoCopy(updatedEvent, eventsPerStream, exprEvaluatorContext);
            instance.handleRowUpdateKeyAfterUpdate(updatedEvent);
        }

        // add to affected indexes
        try {
            for (String affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName);
                index.add(events, exprEvaluatorContext);
            }
        } catch (EPException ex) {
            // rollback
            // remove updated events
            for (String affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName);
                index.remove(events, exprEvaluatorContext);
            }
            // rollback change to events
            for (int i = 0; i < events.length; i++) {
                ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) events[i];
                oa.setPropertyValues(previousData[i]);
            }
            // add old events
            for (String affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName);
                index.add(events, exprEvaluatorContext);
            }
            throw ex;
        }
    }
}
