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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class SelectEvalStreamBaseObjectArray extends SelectEvalStreamBase {

    public SelectEvalStreamBaseObjectArray(SelectExprForgeContext context, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(context, resultEventType, namedStreams, usingWildcard);
    }

    protected abstract CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpressionRef props, CodegenClassScope codegenClassScope);

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        int size = computeSize();
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "props", newArrayByLength(Object.class, constant(size)));
        int count = 0;
        for (ExprForge forge : this.context.getExprForges()) {
            block.assignArrayElement(ref("props"), constant(count), CodegenLegoMayVoid.expressionMayVoid(Object.class, forge, methodNode, exprSymbol, codegenClassScope));
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            CodegenExpression theEvent = arrayAtIndex(refEPS, constant(element.getStreamNumber()));
            block.assignArrayElement(ref("props"), constant(count), theEvent);
            count++;
        }
        if (isUsingWildcard && this.context.getNumStreams() > 1) {
            for (int i = 0; i < this.context.getNumStreams(); i++) {
                block.assignArrayElement(ref("props"), constant(count), arrayAtIndex(refEPS, constant(i)));
                count++;
            }
        }
        block.methodReturn(processSpecificCodegen(resultEventType, eventBeanFactory, ref("props"), codegenClassScope));
        return methodNode;
    }

    private int computeSize() {
        // Evaluate all expressions and build a map of name-value pairs
        int size = (isUsingWildcard && context.getNumStreams() > 1) ? context.getNumStreams() : 0;
        size += context.getExprForges().length + namedStreams.size();
        return size;
    }
}
