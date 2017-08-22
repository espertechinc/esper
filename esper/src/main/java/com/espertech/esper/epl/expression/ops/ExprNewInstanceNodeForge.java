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
import com.espertech.esper.epl.expression.core.ExprNodeRenderable;
import com.espertech.esper.event.bean.InstanceManufacturerFactory;

public class ExprNewInstanceNodeForge implements ExprForge {

    private final ExprNewInstanceNode parent;
    private final Class targetClass;
    private final InstanceManufacturerFactory manufacturerFactory;

    public ExprNewInstanceNodeForge(ExprNewInstanceNode parent, Class targetClass, InstanceManufacturerFactory manufacturerFactory) {
        this.parent = parent;
        this.targetClass = targetClass;
        this.manufacturerFactory = manufacturerFactory;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprNewInstanceNodeForgeEval(this, manufacturerFactory.makeEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return manufacturerFactory.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return targetClass;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }
}
