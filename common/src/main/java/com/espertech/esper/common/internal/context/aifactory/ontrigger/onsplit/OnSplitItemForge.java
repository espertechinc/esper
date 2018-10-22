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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class OnSplitItemForge {
    private final ExprNode whereClause;
    private final boolean isNamedWindowInsert;
    private final TableMetaData insertIntoTable;
    private final ResultSetProcessorDesc resultSetProcessorDesc;
    private final PropertyEvaluatorForge propertyEvaluator;
    private String resultSetProcessorClassName;

    public OnSplitItemForge(ExprNode whereClause, boolean isNamedWindowInsert, TableMetaData insertIntoTable, ResultSetProcessorDesc resultSetProcessorDesc, PropertyEvaluatorForge propertyEvaluator) {
        this.whereClause = whereClause;
        this.isNamedWindowInsert = isNamedWindowInsert;
        this.insertIntoTable = insertIntoTable;
        this.resultSetProcessorDesc = resultSetProcessorDesc;
        this.propertyEvaluator = propertyEvaluator;
    }

    public ExprNode getWhereClause() {
        return whereClause;
    }

    public boolean isNamedWindowInsert() {
        return isNamedWindowInsert;
    }

    public TableMetaData getInsertIntoTable() {
        return insertIntoTable;
    }

    public ResultSetProcessorDesc getResultSetProcessorDesc() {
        return resultSetProcessorDesc;
    }

    public PropertyEvaluatorForge getPropertyEvaluator() {
        return propertyEvaluator;
    }

    public void setResultSetProcessorClassName(String resultSetProcessorClassName) {
        this.resultSetProcessorClassName = resultSetProcessorClassName;
    }

    public static CodegenExpression make(OnSplitItemForge[] items, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[items.length];
        for (int i = 0; i < items.length; i++) {
            expressions[i] = items[i].make(parent, symbols, classScope);
        }
        return newArrayWithInit(OnSplitItemEval.class, expressions);
    }

    private CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(OnSplitItemEval.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(OnSplitItemEval.class, "eval", newInstance(OnSplitItemEval.class))
                .exprDotMethod(ref("eval"), "setWhereClause", whereClause == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(whereClause.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("eval"), "setNamedWindowInsert", constant(isNamedWindowInsert))
                .exprDotMethod(ref("eval"), "setInsertIntoTable", insertIntoTable == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(insertIntoTable, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("eval"), "setRspFactoryProvider", CodegenExpressionBuilder.newInstance(resultSetProcessorClassName, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("eval"), "setPropertyEvaluator", propertyEvaluator == null ? constantNull() : propertyEvaluator.make(method, symbols, classScope))
                .methodReturn(ref("eval"));
        return localMethod(method);
    }
}
