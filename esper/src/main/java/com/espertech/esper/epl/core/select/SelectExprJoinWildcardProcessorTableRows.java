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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
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

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock().declareVar(EventBean[].class, "eventsPerStreamWTableRows", newArrayByLength(EventBean.class, constant(types.length)));
        for (int i = 0; i < types.length; i++) {
            if (tables[i] == null) {
                methodNode.getBlock().assignArrayElement("eventsPerStreamWTableRows", constant(i), arrayAtIndex(refEPS, constant(i)));
            } else {
                CodegenMember eventToPublic = codegenClassScope.makeAddMember(TableMetadataInternalEventToPublic.class, tables[i].getEventToPublic());
                String refname = "e" + i;
                methodNode.getBlock().declareVar(EventBean.class, refname, arrayAtIndex(refEPS, constant(i)))
                        .ifRefNotNull(refname)
                        .assignArrayElement("eventsPerStreamWTableRows", constant(i), exprDotMethod(member(eventToPublic.getMemberId()), "convert", ref(refname), refEPS, refIsNewData, refExprEvalCtx))
                        .blockEnd();
            }
        }
        CodegenMethodNode innerMethod = innerForge.processCodegen(memberResultEventType, memberEventAdapterService, codegenMethodScope, selectSymbol, exprSymbol, codegenClassScope);
        methodNode.getBlock().assignRef(refEPS.getRef(), ref("eventsPerStreamWTableRows"))
                            .methodReturn(localMethod(innerMethod));
        return methodNode;
    }
}
