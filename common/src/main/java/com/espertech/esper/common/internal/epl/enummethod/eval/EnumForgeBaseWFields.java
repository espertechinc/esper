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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

public abstract class EnumForgeBaseWFields implements EnumForge {

    protected ExprForge innerExpression;
    protected int streamNumLambda;
    protected ObjectArrayEventType fieldEventType;

    public EnumForgeBaseWFields(ExprForge innerExpression, int streamNumLambda, ObjectArrayEventType fieldEventType) {
        this.innerExpression = innerExpression;
        this.streamNumLambda = streamNumLambda;
        this.fieldEventType = fieldEventType;
    }

    public EnumForgeBaseWFields(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType) {
        this(lambda.getBodyForge(), lambda.getStreamCountIncoming(), fieldEventType);
    }

    public int getStreamNumSize() {
        return streamNumLambda + 2;
    }

    public ExprForge getInnerExpression() {
        return innerExpression;
    }

    public int getStreamNumLambda() {
        return streamNumLambda;
    }

    public ObjectArrayEventType getFieldEventType() {
        return fieldEventType;
    }
}
