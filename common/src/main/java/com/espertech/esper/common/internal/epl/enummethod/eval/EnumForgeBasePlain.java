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

public abstract class EnumForgeBasePlain implements EnumForge {

    protected ExprForge innerExpression;
    protected int streamNumLambda;

    public EnumForgeBasePlain(ExprDotEvalParamLambda lambda) {
        this(lambda.getBodyForge(), lambda.getStreamCountIncoming());
    }

    public EnumForgeBasePlain(ExprForge innerExpression, int streamCountIncoming) {
        this(streamCountIncoming);
        this.innerExpression = innerExpression;
    }

    public EnumForgeBasePlain(int streamCountIncoming) {
        this.streamNumLambda = streamCountIncoming;
    }

    public ExprForge getInnerExpression() {
        return innerExpression;
    }

    public int getStreamNumSize() {
        return streamNumLambda + 1;
    }

    public int getStreamNumLambda() {
        return streamNumLambda;
    }
}
