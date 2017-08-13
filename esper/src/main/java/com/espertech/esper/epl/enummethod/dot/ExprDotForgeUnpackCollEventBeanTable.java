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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.expression.dot.ExprDotForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class ExprDotForgeUnpackCollEventBeanTable implements ExprDotForge, ExprDotEval {

    private final EPType typeInfo;
    private final TableMetadata tableMetadata;

    public ExprDotForgeUnpackCollEventBeanTable(EventType type, TableMetadata tableMetadata) {
        this.typeInfo = EPTypeHelper.collectionOfSingleValue(tableMetadata.getPublicEventType().getUnderlyingType());
        this.tableMetadata = tableMetadata;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return convertToTableUnderling(target, tableMetadata.getEventToPublic(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, tableMetadata.getEventToPublic());
        return staticMethod(ExprDotForgeUnpackCollEventBeanTable.class, "convertToTableUnderling", inner, CodegenExpressionBuilder.member(eventToPublic.getMemberId()), params.passEPS(), params.passIsNewData(), params.passEvalCtx());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param target target
     * @param eventToPublic conversion
     * @param eventsPerStream events
     * @param isNewData new data flow
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

    public EPType getTypeInfo() {
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
