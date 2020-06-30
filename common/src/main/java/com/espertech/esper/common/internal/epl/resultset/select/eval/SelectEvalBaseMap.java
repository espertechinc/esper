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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class SelectEvalBaseMap extends SelectEvalBase implements SelectExprProcessorForge {

    protected ExprEvaluator[] evaluators;

    protected SelectEvalBaseMap(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        super(selectExprForgeContext, resultEventType);
    }

    protected abstract CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethod methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
        CodegenBlock block = methodNode.getBlock();
        if (this.context.getExprForges().length == 0) {
            block.declareVar(EPTypePremade.MAP.getEPType(), "props", staticMethod(Collections.class, "emptyMap"));
        } else {
            block.declareVar(EPTypePremade.MAP.getEPType(), "props", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(this.context.getColumnNames().length))));
        }
        for (int i = 0; i < this.context.getColumnNames().length; i++) {
            CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(EPTypePremade.OBJECT.getEPType(), this.context.getExprForges()[i], methodNode, exprSymbol, codegenClassScope);
            block.expression(exprDotMethod(ref("props"), "put", constant(this.context.getColumnNames()[i]), expression));
        }
        block.methodReturn(processSpecificCodegen(resultEventType, eventBeanFactory, ref("props"), methodNode, selectSymbol, exprSymbol, codegenClassScope));
        return methodNode;
    }
}