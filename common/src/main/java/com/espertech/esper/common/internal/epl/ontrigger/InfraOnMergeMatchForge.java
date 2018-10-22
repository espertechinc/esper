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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InfraOnMergeMatchForge {
    private final ExprNode optionalCond;
    private final List<InfraOnMergeActionForge> actions;

    public InfraOnMergeMatchForge(ExprNode optionalCond, List<InfraOnMergeActionForge> actions) {
        this.optionalCond = optionalCond;
        this.actions = actions;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InfraOnMergeMatch.class, this.getClass(), classScope);
        CodegenExpression evaluator = optionalCond == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalCond.getForge(), method, this.getClass(), classScope);
        CodegenExpression actionsList = InfraOnMergeActionForge.makeActions(actions, method, symbols, classScope);
        method.getBlock().methodReturn(newInstance(InfraOnMergeMatch.class, evaluator, actionsList));
        return localMethod(method);
    }
}