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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryRangeInForge extends QueryGraphValueEntryRangeForge {

    private ExprNode exprStart;
    private ExprNode exprEnd;
    private boolean allowRangeReversal; // indicate whether "a between 60 and 50" should return no results (false, equivalent to a>= X and a <=Y) or should return results (true, equivalent to 'between' and 'in')

    public QueryGraphValueEntryRangeInForge(QueryGraphRangeEnum rangeType, ExprNode exprStart, ExprNode exprEnd, boolean allowRangeReversal) {
        super(rangeType);
        if (!rangeType.isRange()) {
            throw new IllegalArgumentException("Range type expected but received " + rangeType.name());
        }
        this.exprStart = exprStart;
        this.exprEnd = exprEnd;
        this.allowRangeReversal = allowRangeReversal;
    }

    public boolean isAllowRangeReversal() {
        return allowRangeReversal;
    }

    public ExprNode getExprStart() {
        return exprStart;
    }

    public ExprNode getExprEnd() {
        return exprEnd;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public ExprNode[] getExpressions() {
        return new ExprNode[]{exprStart, exprEnd};
    }

    protected Class getResultType() {
        return exprStart.getForge().getEvaluationType();
    }

    public CodegenExpression make(Class optCoercionType, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryRange.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "start", ExprNodeUtilityCodegen.codegenEvaluatorWCoerce(exprStart.getForge(), optCoercionType, method, this.getClass(), classScope))
                .declareVar(ExprEvaluator.class, "end", ExprNodeUtilityCodegen.codegenEvaluatorWCoerce(exprEnd.getForge(), optCoercionType, method, this.getClass(), classScope))
                .methodReturn(newInstance(QueryGraphValueEntryRangeIn.class, enumValue(QueryGraphRangeEnum.class, type.name()),
                        ref("start"), ref("end"), constant(allowRangeReversal)));
        return localMethod(method);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryRangeIn.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "start", ExprNodeUtilityCodegen.codegenEvaluator(exprStart.getForge(), method, this.getClass(), classScope))
                .declareVar(ExprEvaluator.class, "end", ExprNodeUtilityCodegen.codegenEvaluator(exprEnd.getForge(), method, this.getClass(), classScope))
                .methodReturn(newInstance(QueryGraphValueEntryRangeIn.class, enumValue(QueryGraphRangeEnum.class, type.name()),
                        ref("start"), ref("end"), constant(allowRangeReversal)));
        return localMethod(method);
    }
}
