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
package com.espertech.esper.common.internal.epl.lookupsubord;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategyFactoryForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubordWMatchExprLookupStrategyIndexedFilteredForge implements SubordWMatchExprLookupStrategyFactoryForge {
    private final ExprForge exprForge;
    private final SubordTableLookupStrategyFactoryForge lookupStrategyFactory;

    public SubordWMatchExprLookupStrategyIndexedFilteredForge(ExprForge exprForge, SubordTableLookupStrategyFactoryForge lookupStrategyFactory) {
        this.exprForge = exprForge;
        this.lookupStrategyFactory = lookupStrategyFactory;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordWMatchExprLookupStrategyFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "eval", ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(exprForge, method, this.getClass(), classScope))
                .declareVar(SubordTableLookupStrategyFactory.class, "lookup", lookupStrategyFactory.make(method, symbols, classScope))
                .methodReturn(newInstance(SubordWMatchExprLookupStrategyIndexedFilteredFactory.class, ref("eval"), ref("lookup")));
        return localMethod(method);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " " + " strategy " + lookupStrategyFactory.toQueryPlan();
    }

    public SubordTableLookupStrategyFactoryForge getOptionalInnerStrategy() {
        return lookupStrategyFactory;
    }
}
