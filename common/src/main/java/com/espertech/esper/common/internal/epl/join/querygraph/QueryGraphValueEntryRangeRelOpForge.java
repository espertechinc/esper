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
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryRangeRelOpForge extends QueryGraphValueEntryRangeForge {

    private final ExprNode expression;
    private final boolean isBetweenPart; // indicate that this is part of a between-clause or in-clause

    public QueryGraphValueEntryRangeRelOpForge(QueryGraphRangeEnum type, ExprNode expression, boolean isBetweenPart) {
        super(type);
        if (type.isRange()) {
            throw new IllegalArgumentException("Invalid ctor for use with ranges");
        }
        this.expression = expression;
        this.isBetweenPart = isBetweenPart;
    }

    public ExprNode getExpression() {
        return expression;
    }

    public boolean isBetweenPart() {
        return isBetweenPart;
    }

    public String toQueryPlan() {
        return getType().getStringOp() + " on " + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expression);
    }

    public ExprNode[] getExpressions() {
        return new ExprNode[]{expression};
    }

    protected Class getResultType() {
        return expression.getForge().getEvaluationType();
    }

    public CodegenExpression make(Class optCoercionType, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryRange.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "expression", ExprNodeUtilityCodegen.codegenEvaluatorWCoerce(expression.getForge(), optCoercionType, method, this.getClass(), classScope))
                .methodReturn(newInstance(QueryGraphValueEntryRangeRelOp.class, enumValue(QueryGraphRangeEnum.class, type.name()),
                        ref("expression"), constant(isBetweenPart)));
        return localMethod(method);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryRangeRelOp.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "expression", ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(expression.getForge(), method, this.getClass(), classScope))
                .methodReturn(newInstance(QueryGraphValueEntryRangeRelOp.class, enumValue(QueryGraphRangeEnum.class, type.name()),
                        ref("expression"), constant(isBetweenPart)));
        return localMethod(method);
    }
}
