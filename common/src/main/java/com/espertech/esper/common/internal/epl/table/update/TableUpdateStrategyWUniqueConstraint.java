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
package com.espertech.esper.common.internal.epl.table.update;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.common.internal.type.NameAndModule;

import java.util.Collection;
import java.util.Set;

public class TableUpdateStrategyWUniqueConstraint implements TableUpdateStrategy {

    private final EventBeanUpdateHelperNoCopy updateHelper;
    private final Set<NameAndModule> affectedIndexNames;

    public TableUpdateStrategyWUniqueConstraint(EventBeanUpdateHelperNoCopy updateHelper, Set<NameAndModule> affectedIndexNames) {
        this.updateHelper = updateHelper;
        this.affectedIndexNames = affectedIndexNames;
    }

    public void updateTable(Collection<EventBean> eventsUnsafeIter, TableInstance instance, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // copy references to array - as it is allowed to pass an index-originating collection
        // and those same indexes are being changed now
        EventBean[] events = new EventBean[eventsUnsafeIter.size()];
        int count = 0;
        for (EventBean event : eventsUnsafeIter) {
            events[count++] = event;
        }

        // remove from affected indexes
        for (NameAndModule affectedIndexName : affectedIndexNames) {
            EventTable index = instance.getIndex(affectedIndexName.getName(), affectedIndexName.getModuleName());
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
            for (NameAndModule affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName.getName(), affectedIndexName.getModuleName());
                index.add(events, exprEvaluatorContext);
            }
        } catch (EPException ex) {
            // rollback
            // remove updated events
            for (NameAndModule affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName.getName(), affectedIndexName.getModuleName());
                index.remove(events, exprEvaluatorContext);
            }
            // rollback change to events
            for (int i = 0; i < events.length; i++) {
                ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) events[i];
                oa.setPropertyValues(previousData[i]);
            }
            // add old events
            for (NameAndModule affectedIndexName : affectedIndexNames) {
                EventTable index = instance.getIndex(affectedIndexName.getName(), affectedIndexName.getModuleName());
                index.add(events, exprEvaluatorContext);
            }
            throw ex;
        }
    }
}
