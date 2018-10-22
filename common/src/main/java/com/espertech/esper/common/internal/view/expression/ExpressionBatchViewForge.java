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
import com.espertech.esper.common.internal.view.core.DataWindowBatchingViewForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

/**
 * Factory for {@link ExpressionBatchView}.
 */
public class ExpressionBatchViewForge extends ExpressionViewForgeBase implements DataWindowBatchingViewForge {
    protected boolean includeTriggeringEvent = true;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        if (parameters.size() != 1 && parameters.size() != 2) {
            String errorMessage = getViewName() + " view requires a single expression as a parameter, or an expression and boolean flag";
            throw new ViewParameterException(errorMessage);
        }
        expiryExpression = parameters.get(0);

        if (parameters.size() > 1) {
            Object result = ViewForgeSupport.evaluateAssertNoProperties(getViewName(), parameters.get(1), 1);
            includeTriggeringEvent = (Boolean) result;
        }
    }

    public Class typeOfFactory() {
        return ExpressionBatchViewFactory.class;
    }

    public String factoryMethod() {
        return "exprbatch";
    }

    protected void makeSetters(CodegenExpressionRef factory, CodegenBlock block) {
        block.exprDotMethod(factory, "setIncludeTriggeringEvent", constant(includeTriggeringEvent));
    }

    public String getViewName() {
        return "Expression-batch";
    }
}
