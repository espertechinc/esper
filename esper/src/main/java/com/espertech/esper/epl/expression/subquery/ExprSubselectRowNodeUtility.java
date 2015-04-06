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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

public class ExprSubselectRowNodeUtility {

    private static final Log log = LogFactory.getLog(ExprSubselectRowNodeUtility.class);

    public static EventBean evaluateFilterExpectSingleMatch(EventBean[] eventsZeroSubselect, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {

        EventBean subSelectResult = null;
        for (EventBean subselectEvent : matchingEvents)
        {
            // Prepare filter expression event list
            eventsZeroSubselect[0] = subselectEvent;

            Boolean pass = (Boolean) parent.filterExpr.evaluate(eventsZeroSubselect, newData, exprEvaluatorContext);
            if ((pass != null) && (pass))
            {
                if (subSelectResult != null)
                {
                    log.warn(parent.getMultirowMessage());
                    return null;
                }
                subSelectResult = subselectEvent;
            }
        }

        return subSelectResult;
    }
}
