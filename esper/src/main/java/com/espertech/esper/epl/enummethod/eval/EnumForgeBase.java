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

import com.espertech.esper.epl.expression.core.ExprForge;

public abstract class EnumForgeBase implements EnumForge {

    protected ExprForge innerExpression;
    protected int streamNumLambda;

    public EnumForgeBase(ExprForge innerExpression, int streamCountIncoming) {
        this(streamCountIncoming);
        this.innerExpression = innerExpression;
    }

    public EnumForgeBase(int streamCountIncoming) {
        this.streamNumLambda = streamCountIncoming;
    }

    public ExprForge getInnerExpression() {
        return innerExpression;
    }

    public int getStreamNumSize() {
        return streamNumLambda + 1;
    }
}
