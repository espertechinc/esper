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
import com.espertech.esper.common.internal.epl.pattern.observer.EventObserver;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverEventEvaluator;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverFactory;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class MyFileExistsObserverFactory implements ObserverFactory {
    protected ExprEvaluator filenameExpression;
    protected MatchedEventConvertor convertor;

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, Object observerState, boolean isFilterChildNonQuitting) {
        EventBean[] events = convertor == null ? null : convertor.convert(beginState);
        Object filename = PatternExpressionUtil.evaluateChecked("File-exists observer ", filenameExpression, events, context.getAgentInstanceContext());
        if (filename == null) {
            throw new EPException("Filename evaluated to null");
        }

        return new MyFileExistsObserver(beginState, observerEventEvaluator, filename.toString());
    }

    public boolean isNonRestarting() {
        return false;
    }

    public ExprEvaluator getFilenameExpression() {
        return filenameExpression;
    }

    public void setFilenameExpression(ExprEvaluator filenameExpression) {
        this.filenameExpression = filenameExpression;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }
}
