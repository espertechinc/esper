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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;
import java.util.Set;

public class TableUpdateStrategyIndexNonUnique implements TableUpdateStrategy {

    private final EventBeanUpdateHelperNoCopy updateHelper;
    private final Set<String> affectedIndexNames;

    public TableUpdateStrategyIndexNonUnique(EventBeanUpdateHelperNoCopy updateHelper, Set<String> affectedIndexNames) {
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
        for (String affectedIndexName : affectedIndexNames) {
            EventTable index = instance.getIndex(affectedIndexName);
            index.remove(events, instance.getAgentInstanceContext());
        }

        // update (no-copy unless original values required)
        for (EventBean event : events) {
            eventsPerStream[0] = event;
            ObjectArrayBackedEventBean updatedEvent = (ObjectArrayBackedEventBean) event;

            // if "initial.property" is part of the assignment expressions, provide initial value event
            if (updateHelper.isRequiresStream2InitialValueEvent()) {
                Object[] prev = new Object[updatedEvent.getProperties().length];
                System.arraycopy(updatedEvent.getProperties(), 0, prev, 0, prev.length);
                eventsPerStream[2] = new ObjectArrayEventBean(prev, updatedEvent.getEventType());
            }

            // apply in-place updates
            updateHelper.updateNoCopy(updatedEvent, eventsPerStream, exprEvaluatorContext);
            instance.handleRowUpdated(updatedEvent);
        }

        // add to affected indexes
        for (String affectedIndexName : affectedIndexNames) {
            EventTable index = instance.getIndex(affectedIndexName);
            index.add(events, instance.getAgentInstanceContext());
        }
    }
}
