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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TableColumnMethodPairForge {
    private final ExprForge[] forges;
    private final int column;
    private final ExprNode aggregationNode;

    public TableColumnMethodPairForge(ExprForge[] forges, int column, ExprNode aggregationNode) {
        this.forges = forges;
        this.column = column;
        this.aggregationNode = aggregationNode;
    }

    public int getColumn() {
        return column;
    }

    public ExprNode getAggregationNode() {
        return aggregationNode;
    }

    public ExprForge[] getForges() {
        return forges;
    }

    public static CodegenExpression makeArray(TableColumnMethodPairForge[] methodPairs, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression[] inits = new CodegenExpression[methodPairs.length];
        for (int i = 0; i < inits.length; i++) {
            inits[i] = methodPairs[i].make(method, symbols, classScope);
        }
        return newArrayWithInit(TableColumnMethodPairEval.class, inits);
    }

    private CodegenExpression make(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression eval;
        if (forges.length == 0) {
            eval = ExprNodeUtilityCodegen.codegenEvaluator(new ExprConstantNodeImpl((Object) null).getForge(), method, this.getClass(), classScope);
        } else if (forges.length == 1) {
            eval = ExprNodeUtilityCodegen.codegenEvaluator(forges[0], method, this.getClass(), classScope);
        } else {
            eval = ExprNodeUtilityCodegen.codegenEvaluatorObjectArray(forges, method, this.getClass(), classScope);
        }
        return newInstance(TableColumnMethodPairEval.class, eval, constant(column));
    }
}
