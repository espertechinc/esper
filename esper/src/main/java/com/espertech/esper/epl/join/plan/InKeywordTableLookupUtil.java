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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;

import java.util.LinkedHashSet;
import java.util.Set;

public class InKeywordTableLookupUtil {

    public static Set<EventBean> multiIndexLookup(ExprEvaluator evaluator, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, EventTable[] indexes) {
        Object key = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
        boolean first = true;
        Set<EventBean> result = null;

        for (EventTable table : indexes) {

            Set<EventBean> found = ((PropertyIndexedEventTableSingle) table).lookup(key);
            if (found != null && !found.isEmpty()) {
                if (result == null) {
                    result = found;
                } else if (first) {
                    LinkedHashSet<EventBean> copy = new LinkedHashSet<EventBean>();
                    copy.addAll(result);
                    copy.addAll(found);
                    result = copy;
                    first = false;
                } else {
                    result.addAll(found);
                }
            }
        }

        return result;
    }

    public static Set<EventBean> singleIndexLookup(ExprEvaluator[] evaluators, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, PropertyIndexedEventTableSingle index) {
        boolean first = true;
        Set<EventBean> result = null;

        for (ExprEvaluator evaluator : evaluators) {
            Object key = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            Set<EventBean> found = index.lookup(key);
            if (found != null && !found.isEmpty()) {
                if (result == null) {
                    result = found;
                } else if (first) {
                    LinkedHashSet<EventBean> copy = new LinkedHashSet<EventBean>();
                    copy.addAll(result);
                    copy.addAll(found);
                    result = copy;
                    first = false;
                } else {
                    result.addAll(found);
                }
            }
        }

        return result;
    }
}