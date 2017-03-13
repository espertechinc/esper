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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.type.RelationalOpEnum;

public abstract class SubselectEvalStrategyNRRelOpBase extends SubselectEvalStrategyNRBase {
    protected final RelationalOpEnum.Computer computer;

    public SubselectEvalStrategyNRRelOpBase(ExprEvaluator valueEval, ExprEvaluator selectEval, boolean resultWhenNoMatchingEvents, RelationalOpEnum.Computer computer) {
        super(valueEval, selectEval, resultWhenNoMatchingEvents);
        this.computer = computer;
    }
}
