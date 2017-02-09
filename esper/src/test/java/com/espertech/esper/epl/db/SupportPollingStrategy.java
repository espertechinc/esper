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
package com.espertech.esper.epl.db;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;
import java.util.Map;

public class SupportPollingStrategy implements PollExecStrategy {
    private Map<MultiKey<Object>, List<EventBean>> results;

    public SupportPollingStrategy(Map<MultiKey<Object>, List<EventBean>> results) {
        this.results = results;
    }

    public void start() {

    }

    public List<EventBean> poll(Object[] lookupValues, ExprEvaluatorContext exprEvaluatorContext) {
        return results.get(new MultiKey<Object>(lookupValues));
    }

    public void done() {

    }

    public void destroy() {

    }
}
