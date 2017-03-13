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
import com.espertech.esper.util.SimpleNumberCoercer;

/**
 * Represents a in-subselect evaluation strategy.
 */
public abstract class SubselectEvalStrategyNREqualsInBase extends SubselectEvalStrategyNRBase {
    protected final boolean isNotIn;
    protected final SimpleNumberCoercer coercer;

    public SubselectEvalStrategyNREqualsInBase(ExprEvaluator valueEval, ExprEvaluator selectEval, boolean notIn, SimpleNumberCoercer coercer) {
        super(valueEval, selectEval, notIn);
        isNotIn = notIn;
        this.coercer = coercer;
    }
}
