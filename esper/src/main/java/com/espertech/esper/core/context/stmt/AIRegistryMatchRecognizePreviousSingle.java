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
package com.espertech.esper.core.context.stmt;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.rowregex.RegexPartitionStateRandomAccess;

public class AIRegistryMatchRecognizePreviousSingle implements AIRegistryMatchRecognizePrevious, RegexExprPreviousEvalStrategy {

    private RegexExprPreviousEvalStrategy strategy;

    public void assignService(int num, RegexExprPreviousEvalStrategy value) {
        this.strategy = value;
    }

    public void deassignService(int num) {
        this.strategy = null;
    }

    public int getAgentInstanceCount() {
        return strategy == null ? 0 : 1;
    }

    public RegexPartitionStateRandomAccess getAccess(ExprEvaluatorContext exprEvaluatorContext) {
        return strategy.getAccess(exprEvaluatorContext);
    }
}
