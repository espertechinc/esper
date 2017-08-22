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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.type.BitWiseOpEnum;

public class ExprBitWiseNodeForge implements ExprForge {
    private final ExprBitWiseNode parent;
    private final Class resultType;
    private final BitWiseOpEnum.Computer computer;

    public ExprBitWiseNodeForge(ExprBitWiseNode parent, Class resultType, BitWiseOpEnum.Computer computer) {
        this.parent = parent;
        this.resultType = resultType;
        this.computer = computer;
    }

    public ExprBitWiseNode getForgeRenderable() {
        return parent;
    }

    public BitWiseOpEnum.Computer getComputer() {
        return computer;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprBitWiseNodeForgeEval(this, parent.getChildNodes()[0].getForge().getExprEvaluator(), parent.getChildNodes()[1].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprBitWiseNodeForgeEval.codegen(this, requiredType, codegenMethodScope, exprSymbol, codegenClassScope, parent.getChildNodes()[0], parent.getChildNodes()[1]);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }
}
