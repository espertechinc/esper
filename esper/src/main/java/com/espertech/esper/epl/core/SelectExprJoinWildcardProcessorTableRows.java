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
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;
import com.espertech.esper.epl.table.mgmt.TableService;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorTableRows implements SelectExprProcessor, SelectExprProcessorForge {
    private final SelectExprProcessorForge innerForge;
    private final TableMetadata[] tables;
    private final EventType[] types;

    private SelectExprProcessor inner;

    public SelectExprJoinWildcardProcessorTableRows(EventType[] types, SelectExprProcessorForge inner, TableService tableService) {
        this.types = types;
        this.innerForge = inner;
        tables = new TableMetadata[types.length];
        for (int i = 0; i < types.length; i++) {
            tables[i] = tableService.getTableMetadataFromEventType(types[i]);
        }
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] eventsPerStreamWTableRows = new EventBean[types.length];
        for (int i = 0; i < eventsPerStreamWTableRows.length; i++) {
            if (tables[i] != null && eventsPerStream[i] != null) {
                eventsPerStreamWTableRows[i] = tables[i].getEventToPublic().convert(eventsPerStream[i], eventsPerStream, isNewData, exprEvaluatorContext);
            } else {
                eventsPerStreamWTableRows[i] = eventsPerStream[i];
            }
        }
        return inner.process(eventsPerStreamWTableRows, isNewData, isSynthesize, exprEvaluatorContext);
    }

    public EventType getResultEventType() {
        return innerForge.getResultEventType();
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        this.inner = innerForge.getSelectExprProcessor(engineImportService, isFireAndForget, statementName);
        return this;
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(EventBean.class, SelectExprJoinWildcardProcessorTableRows.class).add(params).begin()
                .declareVar(EventBean[].class, "eventsPerStreamWTableRows", newArray(EventBean.class, constant(types.length)));
        for (int i = 0; i < types.length; i++) {
            if (tables[i] == null) {
                block.assignArrayElement("eventsPerStreamWTableRows", constant(i), arrayAtIndex(params.passEPS(), constant(i)));
            } else {
                CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, tables[i].getEventToPublic());
                String refname = "e" + i;
                block.declareVar(EventBean.class, refname, arrayAtIndex(params.passEPS(), constant(i)))
                        .ifRefNotNull(refname)
                        .assignArrayElement("eventsPerStreamWTableRows", constant(i), exprDotMethod(member(eventToPublic.getMemberId()), "convert", ref(refname), params.passEPS(), params.passIsNewData(), params.passEvalCtx()))
                        .blockEnd();
            }
        }
        CodegenMethodId method = block.assignRef(CodegenParamSetSelectPremade.EPS_NAME, ref("eventsPerStreamWTableRows"))
                            .methodReturn(innerForge.processCodegen(memberResultEventType, memberEventAdapterService, params, context));
        return localMethodBuild(method).passAll(params).call();
    }
}
