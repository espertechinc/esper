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

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class InfraOnMergeActionForge {

    protected final ExprNode optionalFilter;

    public InfraOnMergeActionForge(ExprNode optionalFilter) {
        this.optionalFilter = optionalFilter;
    }

    protected abstract CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    protected CodegenExpression makeFilter(CodegenMethod method, CodegenClassScope classScope) {
        return optionalFilter == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalFilter.getForge(), method, this.getClass(), classScope);
    }

    public static CodegenExpression makeActions(List<InfraOnMergeActionForge> actions, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(List.class, InfraOnMergeActionForge.class, classScope);
        method.getBlock().declareVar(List.class, InfraOnMergeAction.class, "list", newInstance(ArrayList.class, constant(actions.size())));
        for (InfraOnMergeActionForge item : actions) {
            method.getBlock().exprDotMethod(ref("list"), "add", item.make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("list"));
        return localMethod(method);
    }
}