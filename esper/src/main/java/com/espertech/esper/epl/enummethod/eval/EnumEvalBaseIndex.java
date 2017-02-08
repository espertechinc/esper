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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.event.arr.ObjectArrayEventType;

public abstract class EnumEvalBaseIndex implements EnumEval {

    protected ExprEvaluator innerExpression;
    protected int streamNumLambda;
    protected ObjectArrayEventType indexEventType;

    public EnumEvalBaseIndex(ExprEvaluator innerExpression, int streamNumLambda, ObjectArrayEventType indexEventType) {
        this.innerExpression = innerExpression;
        this.streamNumLambda = streamNumLambda;
        this.indexEventType = indexEventType;
    }

    public int getStreamNumSize() {
        return streamNumLambda + 2;
    }
}
