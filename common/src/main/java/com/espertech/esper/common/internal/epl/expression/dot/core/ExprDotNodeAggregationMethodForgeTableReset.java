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
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeAggregationMethodForgeTableReset extends ExprDotNodeAggregationMethodForge {
    private final ExprTableIdentNode identNode;
    private final TableMetadataColumnAggregation column;

    public ExprDotNodeAggregationMethodForgeTableReset(ExprDotNodeImpl parent, String aggregationMethodName, ExprNode[] parameters, AggregationPortableValidation validation, ExprTableIdentNode identNode, TableMetadataColumnAggregation column) {
        super(parent, aggregationMethodName, parameters, validation);
        this.identNode = identNode;
        this.column = column;
    }

    public CodegenExpression evaluateCodegen(String readerMethodName, Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(void.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationRow.class, "row", staticMethod(ExprTableIdentNode.class, "tableColumnRow", constant(identNode.getStreamNum()), symbols.getAddEPS(method)))
            .ifRefNotNull("row")
            .exprDotMethod(ref("row"), "reset", constant(column.getColumn()));
        return localMethod(method);
    }

    protected void toEPL(StringWriter writer) {
        identNode.toPrecedenceFreeEPL(writer);
    }

    protected String getTableName() {
        return identNode.getTableMetadata().getTableName();
    }

    protected String getTableColumnName() {
        return column.getColumnName();
    }
}
