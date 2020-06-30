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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeRenderableFlags;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeAggregationMethodForgeTableIdent extends ExprDotNodeAggregationMethodForge {
    private final ExprTableIdentNode identNode;
    private final TableMetadataColumnAggregation column;

    public ExprDotNodeAggregationMethodForgeTableIdent(ExprDotNodeImpl parent, String aggregationMethodName, ExprNode[] parameters, AggregationPortableValidation validation, ExprTableIdentNode identNode, TableMetadataColumnAggregation column) {
        super(parent, aggregationMethodName, parameters, validation);
        this.identNode = identNode;
        this.column = column;
    }

    public CodegenExpression evaluateCodegen(String readerMethodName, EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(requiredType, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationRow.EPTYPE, "row", staticMethod(ExprTableIdentNode.class, "tableColumnRow", constant(identNode.getStreamNum()), symbols.getAddEPS(method)))
                .ifRefNullReturnNull("row")
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(requiredType, exprDotMethod(getReader(classScope), readerMethodName, constant(column.getColumn()), ref("row"), symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method))));
        return localMethod(method);
    }

    protected void toEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        identNode.toPrecedenceFreeEPL(writer, flags);
    }

    protected String getTableName() {
        return identNode.getTableMetadata().getTableName();
    }

    protected String getTableColumnName() {
        return column.getColumnName();
    }

    public boolean isLocalInlinedClass() {
        return false;
    }
}
