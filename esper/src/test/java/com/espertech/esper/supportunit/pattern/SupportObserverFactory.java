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
package com.espertech.esper.supportunit.pattern;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.pattern.EvalStateNodeNumber;
import com.espertech.esper.pattern.MatchedEventConvertor;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.observer.EventObserver;
import com.espertech.esper.pattern.observer.ObserverEventEvaluator;
import com.espertech.esper.pattern.observer.ObserverFactory;
import com.espertech.esper.pattern.observer.ObserverParameterException;

import java.util.List;

public class SupportObserverFactory implements ObserverFactory {
    public void setObserverParameters(List<ExprNode> observerParameters, MatchedEventConvertor convertor, ExprValidationContext validationContext) throws ObserverParameterException {
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, EvalStateNodeNumber stateNodeId, Object observerState, boolean isFilterChildNonQuitting) {
        return null;
    }

    public boolean isNonRestarting() {
        return false;
    }
}
