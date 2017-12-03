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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.*;
import com.espertech.esper.pattern.observer.EventObserver;
import com.espertech.esper.pattern.observer.ObserverEventEvaluator;
import com.espertech.esper.pattern.observer.ObserverFactorySupport;
import com.espertech.esper.pattern.observer.ObserverParameterException;

import java.util.List;

public class MyFileExistsObserverFactory extends ObserverFactorySupport {
    protected ExprNode filenameExpression;
    protected MatchedEventConvertor convertor;

    public void setObserverParameters(List<ExprNode> expressionParameters, MatchedEventConvertor convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        String message = "File exists observer takes a single string filename parameter";
        if (expressionParameters.size() != 1) {
            throw new ObserverParameterException(message);
        }
        if (!(expressionParameters.get(0).getForge().getEvaluationType() == String.class)) {
            throw new ObserverParameterException(message);
        }

        this.filenameExpression = expressionParameters.get(0);
        this.convertor = convertor;
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, EvalStateNodeNumber stateNodeId, Object observerState, boolean isFilterChildNonQuitting) {
        Object filename = PatternExpressionUtil.evaluate("File-exists observer ", beginState, filenameExpression, convertor, null);
        if (filename == null) {
            throw new EPException("Filename evaluated to null");
        }

        return new MyFileExistsObserver(beginState, observerEventEvaluator, filename.toString());
    }

    public boolean isNonRestarting() {
        return false;
    }
}
