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
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.*;
import com.espertech.esper.pattern.guard.Guard;
import com.espertech.esper.pattern.guard.GuardFactorySupport;
import com.espertech.esper.pattern.guard.GuardParameterException;
import com.espertech.esper.pattern.guard.Quitable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MyCountToPatternGuardFactory extends GuardFactorySupport {
    private static final Logger log = LoggerFactory.getLogger(MyCountToPatternGuardFactory.class);

    private ExprNode numCountToExpr;
    private MatchedEventConvertor convertor;

    public void setGuardParameters(List<ExprNode> guardParameters, MatchedEventConvertor convertor) throws GuardParameterException {
        String message = "Count-to guard takes a single integer-value expression as parameter";
        if (guardParameters.size() != 1) {
            throw new GuardParameterException(message);
        }

        Class paramType = guardParameters.get(0).getForge().getEvaluationType();
        if (paramType != Integer.class && paramType != int.class) {
            throw new GuardParameterException(message);
        }

        this.numCountToExpr = guardParameters.get(0);
        this.convertor = convertor;
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, EvalStateNodeNumber stateNodeId, Object guardState) {
        Object parameter = PatternExpressionUtil.evaluate("Count-to guard", beginState, numCountToExpr, convertor, null);
        if (parameter == null) {
            throw new EPException("Count-to guard parameter evaluated to a null value");
        }

        Integer numCountTo = (Integer) parameter;
        return new MyCountToPatternGuard(numCountTo, quitable);
    }
}
