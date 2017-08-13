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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.blocks.CodegenLegoMayVoid;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class EvalSelectStreamBaseObjectArray extends EvalSelectStreamBase implements SelectExprProcessor {

    public EvalSelectStreamBaseObjectArray(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
    }

    protected abstract EventBean processSpecific(Object[] props, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    protected abstract CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpressionRef props, CodegenParamSetExprPremade instance, CodegenContext context);

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

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        int size = computeSize();
        CodegenBlock block = context.addMethod(EventBean.class, EvalSelectStreamBaseObjectArray.class).add(params).begin()
                .declareVar(Object[].class, "props", newArray(Object.class, constant(size)));
        int count = 0;
        for (ExprForge forge : this.context.getExprForges()) {
            block.assignArrayElement(ref("props"), constant(count), CodegenLegoMayVoid.expressionMayVoid(forge, CodegenParamSetExprPremade.INSTANCE, context));
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            CodegenExpression theEvent = arrayAtIndex(params.passEPS(), constant(element.getStreamNumber()));
            block.assignArrayElement(ref("props"), constant(count), theEvent);
            count++;
        }
        if (isUsingWildcard && this.context.getNumStreams() > 1) {
            for (int i = 0; i < this.context.getNumStreams(); i++) {
                block.assignArrayElement(ref("props"), constant(count), arrayAtIndex(params.passEPS(), constant(i)));
                count++;
            }
        }
        CodegenMethodId method = block.methodReturn(processSpecificCodegen(memberResultEventType, memberEventAdapterService, ref("props"), CodegenParamSetExprPremade.INSTANCE, context));
        return localMethodBuild(method).passAll(params).call();
    }

    private int computeSize() {
        // Evaluate all expressions and build a map of name-value pairs
        int size = (isUsingWildcard && context.getNumStreams() > 1) ? context.getNumStreams() : 0;
        size += context.getExprForges().length + namedStreams.size();
        return size;
    }
}
