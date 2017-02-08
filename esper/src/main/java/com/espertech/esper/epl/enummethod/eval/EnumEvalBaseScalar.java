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

public abstract class EnumEvalBaseScalar extends EnumEvalBase implements EnumEval {

    protected final ObjectArrayEventType type;

    public EnumEvalBaseScalar(ExprEvaluator innerExpression, int streamCountIncoming, ObjectArrayEventType type) {
        super(innerExpression, streamCountIncoming);
        this.type = type;
    }
}
