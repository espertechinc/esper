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
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnPlain;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExprTableAccessNodeTopLevel extends ExprTableAccessNode implements ExprTypableReturnForge, ExprTypableReturnEval, ExprForge {
    private static final long serialVersionUID = -5475434962878200767L;

    private transient LinkedHashMap<String, Object> eventType;

    public ExprTableAccessNodeTopLevel(String tableName) {
        super(tableName);
    }

    public void setStrategy(ExprTableAccessEvalStrategy strategy) {
        this.strategy = strategy;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return this;
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new UnsupportedOperationException();
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata) throws ExprValidationException {
        validateGroupKeys(tableMetadata, validationContext);
        eventType = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, TableMetadataColumn> entry : tableMetadata.getTableColumns().entrySet()) {
            Class classResult;
            if (entry.getValue() instanceof TableMetadataColumnPlain) {
                classResult = tableMetadata.getInternalEventType().getPropertyType(entry.getKey());
            } else {
                TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) entry.getValue();
                classResult = JavaClassHelper.getBoxedType(aggcol.getFactory().getResultType());
            }
            eventType.put(entry.getKey(), classResult);
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTableTop(this, tableName);
            Object result = strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            InstrumentationHelper.get().aExprTableTop(result);
            return result;
        }
        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public Class getEvaluationType() {
        return Map.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return eventType;
    }

    public Boolean isMultirow() {
        return false;
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateTypableSingle(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfTypableSingle(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new UnsupportedOperationException();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer);
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        return true;
    }
}
