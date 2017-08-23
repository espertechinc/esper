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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.util.JavaClassHelper;

public class SelectExprProcessorEnumerationAtBeanCollTableForge implements ExprForge {
    protected final ExprEnumerationForge enumerationForge;
    protected final TableMetadata tableMetadata;

    public SelectExprProcessorEnumerationAtBeanCollTableForge(ExprEnumerationForge enumerationForge, TableMetadata tableMetadata) {
        this.enumerationForge = enumerationForge;
        this.tableMetadata = tableMetadata;
    }

    public ExprEvaluator getExprEvaluator() {
        return new SelectExprProcessorEnumerationAtBeanCollTableEval(this, enumerationForge.getExprEvaluatorEnumeration());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return SelectExprProcessorEnumerationAtBeanCollTableEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return JavaClassHelper.getArrayType(tableMetadata.getPublicEventType().getUnderlyingType());
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }
}
