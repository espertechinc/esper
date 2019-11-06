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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.table.core.TableMetadataInternalEventToPublic;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalEnumerationAtBeanCollTable implements ExprForge, SelectExprProcessorTypableForge {
    protected final ExprEnumerationForge enumerationForge;
    protected final TableMetaData table;

    public ExprEvalEnumerationAtBeanCollTable(ExprEnumerationForge enumerationForge, TableMetaData table) {
        this.enumerationForge = enumerationForge;
        this.table = table;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
            }
        };
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, codegenClassScope, this.getClass());
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean[].class, this.getClass(), codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        methodNode.getBlock()
                .declareVar(Object.class, "result", enumerationForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(ExprEvalEnumerationAtBeanCollTable.class, "convertToTableType", ref("result"), eventToPublic, refEPS, refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);

    }

    public Class getUnderlyingEvaluationType() {
        return JavaClassHelper.getArrayType(table.getPublicEventType().getUnderlyingType());
    }

    public Class getEvaluationType() {
        return EventBean[].class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param result               result
     * @param eventToPublic        conversion
     * @param eventsPerStream      events
     * @param isNewData            flag
     * @param exprEvaluatorContext context
     * @return beans
     */
    public static EventBean[] convertToTableType(Object result, TableMetadataInternalEventToPublic eventToPublic, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (result instanceof Collection) {
            Collection<EventBean> events = (Collection<EventBean>) result;
            EventBean[] out = new EventBean[events.size()];
            int index = 0;
            for (EventBean event : events) {
                out[index++] = eventToPublic.convert(event, eventsPerStream, isNewData, exprEvaluatorContext);
            }
            return out;
        }
        EventBean[] events = (EventBean[]) result;
        for (int i = 0; i < events.length; i++) {
            events[i] = eventToPublic.convert(events[i], eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return events;
    }
}
