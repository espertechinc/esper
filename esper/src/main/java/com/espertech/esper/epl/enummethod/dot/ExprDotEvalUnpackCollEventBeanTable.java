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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

import java.util.ArrayDeque;
import java.util.Collection;

public class ExprDotEvalUnpackCollEventBeanTable implements ExprDotEval {

    private final EPType typeInfo;
    private final TableMetadata tableMetadata;

    public ExprDotEvalUnpackCollEventBeanTable(EventType type, TableMetadata tableMetadata) {
        this.typeInfo = EPTypeHelper.collectionOfSingleValue(tableMetadata.getPublicEventType().getUnderlyingType());
        this.tableMetadata = tableMetadata;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        Collection<EventBean> events = (Collection<EventBean>) target;
        ArrayDeque<Object> underlyings = new ArrayDeque<Object>(events.size());
        for (EventBean event : events) {
            underlyings.add(tableMetadata.getEventToPublic().convertToUnd(event, eventsPerStream, isNewData, exprEvaluatorContext));
        }
        return underlyings;
    }

    public EPType getTypeInfo() {
        return typeInfo;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEventColl();
    }
}
