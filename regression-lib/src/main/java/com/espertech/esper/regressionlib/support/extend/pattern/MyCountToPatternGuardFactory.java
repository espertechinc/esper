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
package com.espertech.esper.regressionlib.support.extend.pattern;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.epl.pattern.guard.Guard;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardFactory;
import com.espertech.esper.common.internal.epl.pattern.guard.Quitable;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyCountToPatternGuardFactory implements GuardFactory {
    private static final Logger log = LoggerFactory.getLogger(MyCountToPatternGuardFactory.class);

    private ExprEvaluator numCountToExpr;
    private MatchedEventConvertor convertor;

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, Object guardState) {
        EventBean[] events = convertor == null ? null : convertor.convert(beginState);
        Object parameter = PatternExpressionUtil.evaluateChecked("Count-to guard", numCountToExpr, events, context.getAgentInstanceContext());
        if (parameter == null) {
            throw new EPException("Count-to guard parameter evaluated to a null value");
        }

        Integer numCountTo = (Integer) parameter;
        return new MyCountToPatternGuard(numCountTo, quitable);
    }

    public ExprEvaluator getNumCountToExpr() {
        return numCountToExpr;
    }

    public void setNumCountToExpr(ExprEvaluator numCountToExpr) {
        this.numCountToExpr = numCountToExpr;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }
}
