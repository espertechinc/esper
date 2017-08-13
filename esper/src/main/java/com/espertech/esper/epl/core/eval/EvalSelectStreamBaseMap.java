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
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;
import com.espertech.esper.util.CollectionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class EvalSelectStreamBaseMap extends EvalSelectStreamBase implements SelectExprProcessorForge, SelectExprProcessor {

    protected EvalSelectStreamBaseMap(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
    }

    protected abstract EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    protected abstract CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenParamSetExprPremade params, CodegenContext context);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        // Evaluate all expressions and build a map of name-value pairs
        Map<String, Object> props = new HashMap<String, Object>();
        int count = 0;
        for (ExprEvaluator expressionNode : evaluators) {
            Object evalResult = expressionNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            props.put(context.getColumnNames()[count], evalResult);
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            EventBean theEvent = eventsPerStream[element.getStreamNumber()];
            if (element.getTableMetadata() != null) {
                if (theEvent != null) {
                    theEvent = element.getTableMetadata().getEventToPublic().convert(theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
                }
            }
            props.put(context.getColumnNames()[count], theEvent);
            count++;
        }
        if (isUsingWildcard && eventsPerStream.length > 1) {
            for (EventBean anEventsPerStream : eventsPerStream) {
                props.put(context.getColumnNames()[count], anEventsPerStream);
                count++;
            }
        }

        return processSpecific(props, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        int size = this.context.getExprForges().length + namedStreams.size() + (isUsingWildcard && this.context.getNumStreams() > 1 ? this.context.getNumStreams() : 0);
        CodegenBlock block = context.addMethod(EventBean.class, EvalSelectStreamBaseMap.class).add(params).begin()
                .declareVar(Map.class, "props", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(size))));
        int count = 0;
        for (ExprForge forge : this.context.getExprForges()) {
            block.expression(exprDotMethod(ref("props"), "put", constant(this.context.getColumnNames()[count]), CodegenLegoMayVoid.expressionMayVoid(forge, CodegenParamSetExprPremade.INSTANCE, context)));
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            CodegenExpression theEvent = arrayAtIndex(params.passEPS(), constant(element.getStreamNumber()));
            if (element.getTableMetadata() != null) {
                CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, element.getTableMetadata().getEventToPublic());
                theEvent = exprDotMethod(member(eventToPublic.getMemberId()), "convert", theEvent, params.passEPS(), params.passIsNewData(), params.passEvalCtx());
            }
            block.expression(exprDotMethod(ref("props"), "put", constant(this.context.getColumnNames()[count]), theEvent));
            count++;
        }
        if (isUsingWildcard && this.context.getNumStreams() > 1) {
            for (int i = 0; i < this.context.getNumStreams(); i++) {
                block.expression(exprDotMethod(ref("props"), "put", constant(this.context.getColumnNames()[count]), arrayAtIndex(params.passEPS(), constant(i))));
                count++;
            }
        }
        CodegenMethodId method = block.methodReturn(processSpecificCodegen(memberResultEventType, memberEventAdapterService, ref("props"), CodegenParamSetExprPremade.INSTANCE, context));
        return localMethodBuild(method).passAll(params).call();
    }
}