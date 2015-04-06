/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.event.EventBeanUtility;

import java.util.Collection;

public class ExprSubselectRowEvalStrategyFilteredUnselectedTable extends ExprSubselectRowEvalStrategyFilteredUnselected {

    private final TableMetadata tableMetadata;

    public ExprSubselectRowEvalStrategyFilteredUnselectedTable(TableMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    @Override
    public Object evaluate(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {
        EventBean[] eventsZeroBased = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        EventBean subSelectResult = ExprSubselectRowNodeUtility.evaluateFilterExpectSingleMatch(eventsZeroBased, newData, matchingEvents, exprEvaluatorContext, parent);
        if (subSelectResult == null) {
            return null;
        }
        return tableMetadata.getEventToPublic().convertToUnd(subSelectResult, eventsPerStream, newData, exprEvaluatorContext);
    }
}
