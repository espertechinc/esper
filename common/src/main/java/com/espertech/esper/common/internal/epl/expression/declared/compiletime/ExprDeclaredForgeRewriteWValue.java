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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDeclaredForgeRewriteWValue extends ExprDeclaredForgeBase {
    private final int[] streamAssignments;
    private final ObjectArrayEventType valueEventType;
    private final List<ExprNode> valueExpressions;
    private ExprEvaluator[] evaluators;

    public ExprDeclaredForgeRewriteWValue(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, boolean audit, String statementName, int[] streamAssignments, ObjectArrayEventType valueEventType, List<ExprNode> valueExpressions) {
        super(parent, innerForge, isCache, audit, statementName);
        this.streamAssignments = streamAssignments;
        this.valueEventType = valueEventType;
        this.valueExpressions = valueExpressions;
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eps, boolean isNewData, ExprEvaluatorContext context) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityQuery.getEvaluatorsNoCompile(valueExpressions);
        }

        Object[] props = new Object[valueEventType.getPropertyNames().length];
        for (int i = 0; i < evaluators.length; i++) {
            props[i] = evaluators[i].evaluate(eps, isNewData, context);
        }

        EventBean[] events = new EventBean[streamAssignments.length];
        events[0] = new ObjectArrayEventBean(props, valueEventType);
        for (int i = 0; i < streamAssignments.length; i++) {
            events[i] = eps[streamAssignments[i]];
        }

        return events;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    protected CodegenExpression codegenEventsPerStreamRewritten(CodegenExpression eventsPerStream, CodegenExpression isNewData, CodegenExpression exprEvalCtx, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(EventBean[].class, ExprDeclaredForgeRewriteWValue.class, codegenClassScope)
            .addParam(EventBean[].class, "eps")
            .addParam(boolean.class, "newData")
            .addParam(ExprEvaluatorContext.class, "ctx");
        CodegenExpressionField valueType = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.class, cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(valueEventType, EPStatementInitServices.REF)));

        CodegenMethod methodValueEval = ExprNodeUtilityCodegen.codegenEvalMethodReturnObjectArray(ExprNodeUtilityQuery.getForges(valueExpressions.toArray(new ExprNode[0])), method, ExprDeclaredForgeRewriteWValue.class, codegenClassScope);
        method.getBlock()
            .declareVar(Object[].class, "props", localMethod(methodValueEval, ref("eps"), ref("newData"), ref("ctx")))
            .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(streamAssignments.length)))
            .assignArrayElement("events", constant(0), newInstance(ObjectArrayEventBean.class, ref("props"), valueType));
        for (int i = 1; i < streamAssignments.length; i++) {
            method.getBlock().assignArrayElement("events", constant(i), arrayAtIndex(ref("eps"), constant(streamAssignments[i])));
        }
        method.getBlock().methodReturn(ref("events"));

        return localMethodBuild(method).pass(eventsPerStream).pass(isNewData).pass(exprEvalCtx).call();
    }
}