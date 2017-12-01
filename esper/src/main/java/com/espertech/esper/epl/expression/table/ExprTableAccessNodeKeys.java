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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

import java.io.StringWriter;

public class ExprTableAccessNodeKeys extends ExprTableAccessNode implements ExprEvaluator {

    private static final long serialVersionUID = -1905494870160778124L;

    public ExprTableAccessNodeKeys(String tableName) {
        super(tableName);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer);
        writer.append(".keys()");
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata) throws ExprValidationException {
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluate(eventsPerStream, isNewData, context);
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        return true;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public Class getEvaluationType() {
        return Object[].class;
    }

    public ExprForge getForge() {
        return this;
    }
}
