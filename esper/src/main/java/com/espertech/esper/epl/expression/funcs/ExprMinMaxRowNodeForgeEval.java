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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberBigDecimalCoercer;
import com.espertech.esper.util.SimpleNumberBigIntegerCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents the MAX(a,b) and MIN(a,b) functions is an expression tree.
 */
public class ExprMinMaxRowNodeForgeEval implements ExprEvaluator {

    private final ExprMinMaxRowNodeForge forge;
    private final MinMaxTypeEnum.Computer computer;

    public ExprMinMaxRowNodeForgeEval(ExprMinMaxRowNodeForge forge, ExprEvaluator[] evaluators, ExprForge[] forges) {
        this.forge = forge;
        if (forge.getEvaluationType() == BigInteger.class) {
            SimpleNumberBigIntegerCoercer[] convertors = new SimpleNumberBigIntegerCoercer[evaluators.length];
            for (int i = 0; i < evaluators.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigInteger(forges[i].getEvaluationType());
            }
            computer = new MinMaxTypeEnum.ComputerBigIntCoerce(evaluators, convertors, forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX);
        } else if (forge.getEvaluationType() == BigDecimal.class) {
            SimpleNumberBigDecimalCoercer[] convertors = new SimpleNumberBigDecimalCoercer[evaluators.length];
            for (int i = 0; i < evaluators.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigDecimal(forges[i].getEvaluationType());
            }
            computer = new MinMaxTypeEnum.ComputerBigDecCoerce(evaluators, convertors, forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX);
        } else {
            if (forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX) {
                computer = new MinMaxTypeEnum.MaxComputerDoubleCoerce(evaluators);
            } else {
                computer = new MinMaxTypeEnum.MinComputerDoubleCoerce(evaluators);
            }
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprMinMaxRow(forge.getForgeRenderable());
        }
        Number result = computer.execute(eventsPerStream, isNewData, exprEvaluatorContext);

        if (InstrumentationHelper.ENABLED) {
            Number minmax = null;
            if (result != null) {
                minmax = JavaClassHelper.coerceBoxed(result, forge.getEvaluationType());
            }
            InstrumentationHelper.get().aExprMinMaxRow(minmax);
            return minmax;
        }

        if (result == null) {
            return null;
        }
        return JavaClassHelper.coerceBoxed(result, forge.getEvaluationType());
    }

    public static CodegenExpression codegen(ExprMinMaxRowNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class resultType = forge.getEvaluationType();
        ExprNode[] nodes = forge.getForgeRenderable().getChildNodes();

        CodegenExpression expression;
        if (resultType == BigInteger.class) {
            SimpleNumberBigIntegerCoercer[] convertors = new SimpleNumberBigIntegerCoercer[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigInteger(nodes[i].getForge().getEvaluationType());
            }
            expression = MinMaxTypeEnum.ComputerBigIntCoerce.codegen(forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX, codegenMethodScope, exprSymbol, codegenClassScope, nodes, convertors);
        } else if (resultType == BigDecimal.class) {
            SimpleNumberBigDecimalCoercer[] convertors = new SimpleNumberBigDecimalCoercer[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                convertors[i] = SimpleNumberCoercerFactory.getCoercerBigDecimal(nodes[i].getForge().getEvaluationType());
            }
            expression = MinMaxTypeEnum.ComputerBigDecCoerce.codegen(forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX, codegenMethodScope, exprSymbol, codegenClassScope, nodes, convertors);
        } else {
            if (forge.getForgeRenderable().getMinMaxTypeEnum() == MinMaxTypeEnum.MAX) {
                expression = MinMaxTypeEnum.MaxComputerDoubleCoerce.codegen(codegenMethodScope, exprSymbol, codegenClassScope, nodes, resultType);
            } else {
                expression = MinMaxTypeEnum.MinComputerDoubleCoerce.codegen(codegenMethodScope, exprSymbol, codegenClassScope, nodes, resultType);
            }
        }
        return expression;
    }

}
