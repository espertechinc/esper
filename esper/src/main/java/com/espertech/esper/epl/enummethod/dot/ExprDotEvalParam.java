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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;

public abstract class ExprDotEvalParam {
    private int parameterNum;
    private ExprNode body;
    private ExprForge bodyForge;

    protected ExprDotEvalParam(int parameterNum, ExprNode body, ExprForge bodyForge) {
        this.parameterNum = parameterNum;
        this.body = body;
        this.bodyForge = bodyForge;
    }

    public int getParameterNum() {
        return parameterNum;
    }

    public ExprNode getBody() {
        return body;
    }

    public ExprForge getBodyForge() {
        return bodyForge;
    }
}
