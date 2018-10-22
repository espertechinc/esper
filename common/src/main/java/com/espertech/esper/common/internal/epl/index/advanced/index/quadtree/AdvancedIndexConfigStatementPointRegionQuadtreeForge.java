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

public class AdvancedIndexConfigStatementPointRegionQuadtreeForge implements EventAdvancedIndexConfigStatementForge {
    private final ExprForge xEval;
    private final ExprForge yEval;

    public AdvancedIndexConfigStatementPointRegionQuadtreeForge(ExprForge xEval, ExprForge yEval) {
        this.xEval = xEval;
        this.yEval = yEval;
    }

    public ExprForge getxEval() {
        return xEval;
    }

    public ExprForge getyEval() {
        return yEval;
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AdvancedIndexConfigStatementPointRegionQuadtree.class, this.getClass(), classScope);
        Function<ExprForge, CodegenExpression> expr = forge -> ExprNodeUtilityCodegen.codegenEvaluator(forge, method, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AdvancedIndexConfigStatementPointRegionQuadtree.class, "factory", newInstance(AdvancedIndexConfigStatementPointRegionQuadtree.class))
                .exprDotMethod(ref("factory"), "setxEval", expr.apply(xEval))
                .exprDotMethod(ref("factory"), "setyEval", expr.apply(yEval))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public EventAdvancedIndexConfigStatement toRuntime() {
        AdvancedIndexConfigStatementPointRegionQuadtree cfg = new AdvancedIndexConfigStatementPointRegionQuadtree();
        cfg.setxEval(xEval.getExprEvaluator());
        cfg.setyEval(yEval.getExprEvaluator());
        return cfg;
    }
}
