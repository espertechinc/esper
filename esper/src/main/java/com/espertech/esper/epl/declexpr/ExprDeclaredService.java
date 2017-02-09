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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateExpressionDesc;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;

import java.util.List;

public interface ExprDeclaredService {
    public ExpressionDeclItem getExpression(String name);

    public List<ExpressionScriptProvided> getScriptsByName(String expressionName);

    public String addExpressionOrScript(CreateExpressionDesc expression) throws ExprValidationException;

    public void destroyedExpression(CreateExpressionDesc expression);

    public void destroy();
}
