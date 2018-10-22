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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class ExpressionGuardFactory implements GuardFactory {

    protected ExprEvaluator expression;
    protected MatchedEventConvertor convertor;

    public void setExpression(ExprEvaluator expression) {
        this.expression = expression;
    }

    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, Object guardState) {
        return new ExpressionGuard(convertor, expression, quitable);
    }
}