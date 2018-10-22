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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectEvalJoinWildcardProcessorTableRows implements SelectExprProcessorForge {
    private final SelectExprProcessorForge innerForge;
    private final TableMetaData[] tables;
    private final EventType[] types;

    private SelectExprProcessor inner;

    public SelectEvalJoinWildcardProcessorTableRows(EventType[] types, SelectExprProcessorForge inner, TableCompileTimeResolver tableResolver) {
        this.types = types;
        this.innerForge = inner;
        tables = new TableMetaData[types.length];
        for (int i = 0; i < types.length; i++) {
            tables[i] = tableResolver.resolveTableFromEventType(types[i]);
        }
    }

    public EventType getResultEventType() {
        return innerForge.getResultEventType();
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock().declareVar(EventBean[].class, "eventsPerStreamWTableRows", newArrayByLength(EventBean.class, constant(types.length)));
        for (int i = 0; i < types.length; i++) {
            if (tables[i] == null) {
                methodNode.getBlock().assignArrayElement("eventsPerStreamWTableRows", constant(i), arrayAtIndex(refEPS, constant(i)));
            } else {
                CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(tables[i], codegenClassScope, this.getClass());
                String refname = "e" + i;
                methodNode.getBlock().declareVar(EventBean.class, refname, arrayAtIndex(refEPS, constant(i)))
                        .ifRefNotNull(refname)
                        .assignArrayElement("eventsPerStreamWTableRows", constant(i), exprDotMethod(eventToPublic, "convert", ref(refname), refEPS, refIsNewData, refExprEvalCtx))
                        .blockEnd();
            }
        }
        CodegenMethod innerMethod = innerForge.processCodegen(resultEventType, eventBeanFactory, codegenMethodScope, selectSymbol, exprSymbol, codegenClassScope);
        methodNode.getBlock().assignRef(refEPS.getRef(), ref("eventsPerStreamWTableRows"))
                .methodReturn(localMethod(innerMethod));
        return methodNode;
    }
}
