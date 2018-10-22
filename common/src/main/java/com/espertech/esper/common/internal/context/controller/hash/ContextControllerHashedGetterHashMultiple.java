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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextControllerHashedGetterHashMultiple implements EventPropertyValueGetterForge {
    private static final Logger log = LoggerFactory.getLogger(ContextControllerHashedGetterHashMultiple.class);

    private final ExprNode[] nodes;
    private final int granularity;

    public ContextControllerHashedGetterHashMultiple(List<ExprNode> expressions, int granularity) {
        nodes = ExprNodeUtilityQuery.toArray(expressions);
        this.granularity = granularity;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(EventBean.class, "eventBean");
        method.getBlock().declareVar(EventBean[].class, "events", newArrayWithInit(EventBean.class, ref("eventBean")));

        // method to evaluate expressions and compute hash
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, true);
        CodegenMethod exprMethod = method.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            expressions[i] = nodes[i].getForge().evaluateCodegen(Object.class, exprMethod, exprSymbol, classScope);
        }
        exprSymbol.derivedSymbolsCodegen(method, exprMethod.getBlock(), classScope);

        CodegenExpressionRef hashCode = ref("hashCode");
        exprMethod.getBlock().declareVar(int.class, hashCode.getRef(), constant(0));
        for (int i = 0; i < nodes.length; i++) {
            CodegenExpressionRef result = ref("result" + i);
            exprMethod.getBlock()
                    .declareVar(Object.class, result.getRef(), expressions[i])
                    .ifRefNotNull(result.getRef())
                    .assignRef(hashCode, op(op(constant(31), "*", hashCode), "+", exprDotMethod(result, "hashCode")))
                    .blockEnd();
        }
        exprMethod.getBlock()
                .ifCondition(relational(hashCode, CodegenExpressionRelational.CodegenRelational.GE, constant(0)))
                .blockReturn(op(hashCode, "%", constant(granularity)))
                .methodReturn(op(op(hashCode, "%", constant(granularity)), "*", constant(-1)));

        method.getBlock().methodReturn(localMethod(exprMethod, ref("events"), constantTrue(), constantNull()));
        return localMethod(method, beanExpression);
    }
}
