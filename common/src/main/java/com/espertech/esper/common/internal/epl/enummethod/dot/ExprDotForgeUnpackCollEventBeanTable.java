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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.table.core.TableMetadataInternalEventToPublic;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class ExprDotForgeUnpackCollEventBeanTable implements ExprDotForge, ExprDotEval {

    private final EPChainableType typeInfo;
    private final TableMetaData table;

    public ExprDotForgeUnpackCollEventBeanTable(EventType type, TableMetaData table) {
        this.typeInfo = EPChainableTypeHelper.collectionOfSingleValue(table.getPublicEventType().getUnderlyingEPType());
        this.table = table;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, classScope, this.getClass());
        CodegenExpressionRef refEPS = symbols.getAddEPS(parent);
        CodegenExpression refIsNewData = symbols.getAddIsNewData(parent);
        CodegenExpressionRef refExprEvalCtx = symbols.getAddExprEvalCtx(parent);
        return staticMethod(ExprDotForgeUnpackCollEventBeanTable.class, "convertToTableUnderling", inner, eventToPublic, refEPS, refIsNewData, refExprEvalCtx);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param target               target
     * @param eventToPublic        conversion
     * @param eventsPerStream      events
     * @param isNewData            new data flow
     * @param exprEvaluatorContext context
     * @return events
     */
    public static Collection<Object[]> convertToTableUnderling(Object target, TableMetadataInternalEventToPublic eventToPublic, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        Collection<EventBean> events = (Collection<EventBean>) target;
        ArrayDeque<Object[]> underlyings = new ArrayDeque<>(events.size());
        for (EventBean event : events) {
            underlyings.add(eventToPublic.convertToUnd(event, eventsPerStream, isNewData, exprEvaluatorContext));
        }
        return underlyings;
    }

    public EPChainableType getTypeInfo() {
        return typeInfo;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEventColl();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }
}
