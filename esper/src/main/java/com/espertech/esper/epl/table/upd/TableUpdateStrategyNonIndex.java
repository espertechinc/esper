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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventBean;

import java.util.Collection;

public class TableUpdateStrategyNonIndex implements TableUpdateStrategy {

    private final EventBeanUpdateHelper updateHelper;

    public TableUpdateStrategyNonIndex(EventBeanUpdateHelper updateHelper) {
        this.updateHelper = updateHelper;
    }

    public void updateTable(Collection<EventBean> eventsUnsafeIter, TableStateInstance instance, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // update (no-copy unless original values required)
        for (EventBean event : eventsUnsafeIter) {
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
    }
}
