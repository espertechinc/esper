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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class EvalSelectStreamBaseObjectArray extends EvalSelectStreamBase implements SelectExprProcessor {

    public EvalSelectStreamBaseObjectArray(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
    }

    protected abstract EventBean processSpecific(Object[] props, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    protected abstract CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpressionRef props, CodegenClassScope codegenClassScope);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        int size = computeSize();
        Object[] props = new Object[size];
        int count = 0;
        for (ExprEvaluator expressionNode : evaluators) {
            Object evalResult = expressionNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            props[count] = evalResult;
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            EventBean theEvent = eventsPerStream[element.getStreamNumber()];
            props[count] = theEvent;
            count++;
        }
        if (isUsingWildcard && eventsPerStream.length > 1) {
            for (EventBean anEventsPerStream : eventsPerStream) {
                props[count] = anEventsPerStream;
                count++;
            }
        }

        return processSpecific(props, eventsPerStream, exprEvaluatorContext);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        int size = computeSize();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
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
        block.methodReturn(processSpecificCodegen(memberResultEventType, memberEventAdapterService, ref("props"), codegenClassScope));
        return methodNode;
    }

    private int computeSize() {
        // Evaluate all expressions and build a map of name-value pairs
        int size = (isUsingWildcard && context.getNumStreams() > 1) ? context.getNumStreams() : 0;
        size += context.getExprForges().length + namedStreams.size();
        return size;
    }
}
