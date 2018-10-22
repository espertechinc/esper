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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.TypeWidener;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.lang.annotation.Annotation;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InternalEventRouterDescForge {
    private final EventBeanCopyMethodForge copyMethod;
    private final TypeWidenerSPI[] wideners;
    private final EventType eventType;
    private final Annotation[] annotations;
    private final ExprNode optionalWhereClause;
    private final String[] properties;
    private final ExprNode[] assignments;

    public InternalEventRouterDescForge(EventBeanCopyMethodForge copyMethod, TypeWidenerSPI[] wideners, EventType eventType, Annotation[] annotations, ExprNode optionalWhereClause, String[] properties, ExprNode[] assignments) {
        this.copyMethod = copyMethod;
        this.wideners = wideners;
        this.eventType = eventType;
        this.annotations = annotations;
        this.optionalWhereClause = optionalWhereClause;
        this.properties = properties;
        this.assignments = assignments;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InternalEventRouterDesc.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(InternalEventRouterDesc.class, "ire", newInstance(InternalEventRouterDesc.class))
                .exprDotMethod(ref("ire"), "setWideners", makeWideners(wideners, method, classScope))
                .exprDotMethod(ref("ire"), "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("ire"), "setOptionalWhereClauseEval", optionalWhereClause == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalWhereClause.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("ire"), "setProperties", constant(properties))
                .exprDotMethod(ref("ire"), "setAssignments", ExprNodeUtilityCodegen.codegenEvaluators(assignments, method, this.getClass(), classScope))
                .methodReturn(ref("ire"));
        return localMethod(method);
    }

    private CodegenExpression makeWideners(TypeWidenerSPI[] wideners, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpression[] init = new CodegenExpression[wideners.length];
        for (int i = 0; i < init.length; i++) {
            if (wideners[i] != null) {
                init[i] = TypeWidenerFactory.codegenWidener(wideners[i], method, this.getClass(), classScope);
            } else {
                init[i] = constantNull();
            }
        }
        return newArrayWithInit(TypeWidener.class, init);
    }
}
