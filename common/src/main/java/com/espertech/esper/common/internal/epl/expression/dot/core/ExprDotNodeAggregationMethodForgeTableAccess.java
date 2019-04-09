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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameTableAccess;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeSubprop;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ExprDotNodeAggregationMethodForgeTableAccess extends ExprDotNodeAggregationMethodForge {
    private final ExprTableAccessNodeSubprop subprop;
    private final TableMetadataColumnAggregation column;

    public ExprDotNodeAggregationMethodForgeTableAccess(ExprDotNodeImpl parent, String aggregationMethodName, ExprNode[] parameters, AggregationPortableValidation validation, ExprTableAccessNodeSubprop subprop, TableMetadataColumnAggregation column) {
        super(parent, aggregationMethodName, parameters, validation);
        this.subprop = subprop;
        this.column = column;
    }

    protected CodegenExpression evaluateCodegen(String readerMethodName, Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(requiredType, ExprTableAccessNode.class, classScope);

        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenExpression newData = symbols.getAddIsNewData(method);
        CodegenExpression evalCtx = symbols.getAddExprEvalCtx(method);

        CodegenExpressionField future = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameTableAccess(subprop.getTableAccessNumber()), ExprTableEvalStrategy.class);
        method.getBlock()
            .declareVar(AggregationRow.class, "row", exprDotMethod(future, "getAggregationRow", eps, newData, evalCtx))
            .ifRefNullReturnNull("row")
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(requiredType, exprDotMethod(getReader(classScope), readerMethodName, constant(column.getColumn()), ref("row"), symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method))));
        return localMethod(method);
    }

    protected void toEPL(StringWriter writer) {
        subprop.toPrecedenceFreeEPL(writer);
    }

    protected String getTableName() {
        return subprop.getTableName();
    }

    protected String getTableColumnName() {
        return column.getColumnName();
    }
}
