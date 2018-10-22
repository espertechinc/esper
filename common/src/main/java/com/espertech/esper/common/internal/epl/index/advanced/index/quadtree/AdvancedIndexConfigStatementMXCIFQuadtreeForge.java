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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatement;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatementForge;

import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AdvancedIndexConfigStatementMXCIFQuadtreeForge implements EventAdvancedIndexConfigStatementForge {
    private final ExprForge xEval;
    private final ExprForge yEval;
    private final ExprForge widthEval;
    private final ExprForge heightEval;

    public AdvancedIndexConfigStatementMXCIFQuadtreeForge(ExprForge xEval, ExprForge yEval, ExprForge widthEval, ExprForge heightEval) {
        this.xEval = xEval;
        this.yEval = yEval;
        this.widthEval = widthEval;
        this.heightEval = heightEval;
    }

    public ExprForge getxEval() {
        return xEval;
    }

    public ExprForge getyEval() {
        return yEval;
    }

    public ExprForge getWidthEval() {
        return widthEval;
    }

    public ExprForge getHeightEval() {
        return heightEval;
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AdvancedIndexConfigStatementMXCIFQuadtree.class, this.getClass(), classScope);
        Function<ExprForge, CodegenExpression> expr = forge -> ExprNodeUtilityCodegen.codegenEvaluator(forge, method, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AdvancedIndexConfigStatementMXCIFQuadtree.class, "factory", newInstance(AdvancedIndexConfigStatementMXCIFQuadtree.class))
                .exprDotMethod(ref("factory"), "setxEval", expr.apply(xEval))
                .exprDotMethod(ref("factory"), "setyEval", expr.apply(yEval))
                .exprDotMethod(ref("factory"), "setWidthEval", expr.apply(widthEval))
                .exprDotMethod(ref("factory"), "setHeightEval", expr.apply(heightEval))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public EventAdvancedIndexConfigStatement toRuntime() {
        AdvancedIndexConfigStatementMXCIFQuadtree config = new AdvancedIndexConfigStatementMXCIFQuadtree();
        config.setxEval(xEval.getExprEvaluator());
        config.setyEval(xEval.getExprEvaluator());
        config.setWidthEval(widthEval.getExprEvaluator());
        config.setHeightEval(heightEval.getExprEvaluator());
        return config;
    }
}
