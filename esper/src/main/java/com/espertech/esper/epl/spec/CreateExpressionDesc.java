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
package com.espertech.esper.epl.spec;

import com.espertech.esper.collection.Pair;

import java.io.Serializable;

public class CreateExpressionDesc implements Serializable {
    private static final long serialVersionUID = 7784429868833410090L;
    private final ExpressionDeclItem expression;
    private final ExpressionScriptProvided script;

    public CreateExpressionDesc(ExpressionDeclItem expression) {
        this.expression = expression;
        this.script = null;
    }

    public CreateExpressionDesc(ExpressionScriptProvided script) {
        this.script = script;
        this.expression = null;
    }

    public CreateExpressionDesc(Pair<ExpressionDeclItem, ExpressionScriptProvided> pair) {
        this.script = pair.getSecond();
        this.expression = pair.getFirst();
    }

    public ExpressionDeclItem getExpression() {
        return expression;
    }

    public ExpressionScriptProvided getScript() {
        return script;
    }
}
