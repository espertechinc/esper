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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

public class ExprEventIdentityEqualsNodeForge implements ExprForge {
    private final ExprEventIdentityEqualsNode node;
    private final ExprStreamUnderlyingNode undLeft;
    private final ExprStreamUnderlyingNode undRight;

    public ExprEventIdentityEqualsNodeForge(ExprEventIdentityEqualsNode node, ExprStreamUnderlyingNode undLeft, ExprStreamUnderlyingNode undRight) {
        this.node = node;
        this.undLeft = undLeft;
        this.undRight = undRight;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return ExprEventIdentityEqualsNodeEval.evaluateCodegen(this, parent, symbols, classScope);
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprEventIdentityEqualsNodeEval(undLeft.getStreamId(), undRight.getStreamId());
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return node;
    }

    public ExprStreamUnderlyingNode getUndLeft() {
        return undLeft;
    }

    public ExprStreamUnderlyingNode getUndRight() {
        return undRight;
    }
}
