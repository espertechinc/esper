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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;

import java.util.List;

public class ExpressionWindowViewForge extends ExpressionViewForgeBase {

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        if (parameters.size() != 1) {
            String errorMessage = getViewName() + " view requires a single expression as a parameter";
            throw new ViewParameterException(errorMessage);
        }
        expiryExpression = parameters.get(0);
    }

    public Class typeOfFactory() {
        return ExpressionWindowViewFactory.class;
    }

    protected void makeSetters(CodegenExpressionRef factory, CodegenBlock block) {
    }

    public String factoryMethod() {
        return "expr";
    }

    public String getViewName() {
        return "Expression";
    }
}
