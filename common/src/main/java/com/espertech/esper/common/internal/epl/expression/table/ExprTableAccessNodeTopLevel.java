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
package com.espertech.esper.common.internal.epl.expression.table;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyEnum;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode.AccessEvaluationType.EVALTYPABLESINGLE;

public class ExprTableAccessNodeTopLevel extends ExprTableAccessNode implements ExprTypableReturnForge, ExprTypableReturnEval, ExprForge {

    private LinkedHashMap<String, Object> eventType;

    public ExprTableAccessNodeTopLevel(String tableName) {
        super(tableName);
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return this;
    }

    protected void validateBindingInternal(ExprValidationContext validationContext) throws ExprValidationException {
        validateGroupKeys(tableMeta, validationContext);
        eventType = new LinkedHashMap<>();
        for (Map.Entry<String, TableMetadataColumn> entry : tableMeta.getColumns().entrySet()) {
            Class classResult = tableMeta.getPublicEventType().getPropertyType(entry.getKey());
            eventType.put(entry.getKey(), classResult);
        }
    }

    public ExprTableEvalStrategyFactoryForge getTableAccessFactoryForge() {
        ExprTableEvalStrategyFactoryForge forge = new ExprTableEvalStrategyFactoryForge(tableMeta, groupKeyEvaluators);
        forge.setStrategyEnum(tableMeta.isKeyed() ? ExprTableEvalStrategyEnum.GROUPED_TOP : ExprTableEvalStrategyEnum.UNGROUPED_TOP);
        return forge;
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
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new UnsupportedOperationException();
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return makeEvaluate(EVALTYPABLESINGLE, this, Object[].class, parent, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new UnsupportedOperationException("Typable-multi is not available for table top-level access");
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer);
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        return true;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    protected String getInstrumentationQName() {
        return "ExprTableTop";
    }

    protected CodegenExpression[] getInstrumentationQParams() {
        return new CodegenExpression[]{constant(tableMeta.getTableName())};
    }
}
