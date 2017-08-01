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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectExprProcessorEvalStreamInsertTable implements ExprForge, ExprEvaluator, ExprNodeRenderable {
    private final int streamNum;
    private final ExprStreamUnderlyingNode undNode;
    private final TableMetadata tableMetadata;
    private final Class returnType;

    public SelectExprProcessorEvalStreamInsertTable(int streamNum, ExprStreamUnderlyingNode undNode, TableMetadata tableMetadata, Class returnType) {
        this.streamNum = streamNum;
        this.undNode = undNode;
        this.tableMetadata = tableMetadata;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprStreamUndSelectClause(undNode);
        }
        EventBean event = convertToTableEvent(streamNum, tableMetadata.getEventToPublic(), eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprStreamUndSelectClause(event);
        }
        return event;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, tableMetadata.getEventToPublic());
        return staticMethod(SelectExprProcessorEvalStreamInsertTable.class, "convertToTableEvent", constant(streamNum), ref(eventToPublic.getMemberName()), params.passEPS(), params.passIsNewData(), params.passEvalCtx());
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param streamNum stream num
     * @param eventToPublic conversion
     * @param eventsPerStream events
     * @param isNewData flag
     * @param exprEvaluatorContext context
     * @return event
     */
    public static EventBean convertToTableEvent(int streamNum, TableMetadataInternalEventToPublic eventToPublic, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream == null ? null : eventsPerStream[streamNum];
        if (event != null) {
            event = eventToPublic.convert(event, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return event;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
