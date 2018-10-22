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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContainedEventEvalExprNodeForge implements ContainedEventEvalForge {

    private final ExprForge evaluator;
    private final EventType eventType;

    public ContainedEventEvalExprNodeForge(ExprForge evaluator, EventType eventType) {
        this.evaluator = evaluator;
        this.eventType = eventType;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContainedEventEvalExprNode.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "eval", ExprNodeUtilityCodegen.codegenEvaluator(evaluator, method, this.getClass(), classScope))
                .declareVar(EventType.class, "type", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .methodReturn(newInstance(ContainedEventEvalExprNode.class, ref("eval"), ref("type"), symbols.getAddInitSvc(method)));
        return localMethod(method);
    }
}
