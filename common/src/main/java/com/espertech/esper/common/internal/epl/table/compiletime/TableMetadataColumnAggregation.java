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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.rettype.EPType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TableMetadataColumnAggregation extends TableMetadataColumn {

    private int column;
    private AggregationPortableValidation aggregationPortableValidation;
    private String aggregationExpression;
    private boolean methodAgg;
    private EPType optionalEnumerationType;

    public TableMetadataColumnAggregation() {
    }

    public TableMetadataColumnAggregation(String columnName, boolean key, int column, AggregationPortableValidation aggregationPortableValidation, String aggregationExpression, boolean methodAgg, EPType optionalEnumerationType) {
        super(columnName, key);
        this.column = column;
        this.aggregationPortableValidation = aggregationPortableValidation;
        this.aggregationExpression = aggregationExpression;
        this.methodAgg = methodAgg;
        this.optionalEnumerationType = optionalEnumerationType;
    }

    protected CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TableMetadataColumnAggregation.class, this.getClass(), classScope);
        method.getBlock().declareVar(TableMetadataColumnAggregation.class, "col", newInstance(TableMetadataColumnAggregation.class));

        super.makeSettersInline(ref("col"), method.getBlock());
        method.getBlock()
                .exprDotMethod(ref("col"), "setColumn", constant(column))
                .exprDotMethod(ref("col"), "setAggregationPortableValidation", aggregationPortableValidation.make(method, symbols, classScope))
                .exprDotMethod(ref("col"), "setAggregationExpression", constant(aggregationExpression))
                .exprDotMethod(ref("col"), "setMethodAgg", constant(methodAgg))
                .exprDotMethod(ref("col"), "setOptionalEnumerationType", optionalEnumerationType == null ? constantNull() : optionalEnumerationType.codegen(method, classScope, symbols.getAddInitSvc(method)))
                .methodReturn(ref("col"));
        return localMethod(method);
    }

    public int getColumn() {
        return column;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return aggregationPortableValidation;
    }

    public String getAggregationExpression() {
        return aggregationExpression;
    }

    public boolean isMethodAgg() {
        return methodAgg;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setAggregationPortableValidation(AggregationPortableValidation aggregationPortableValidation) {
        this.aggregationPortableValidation = aggregationPortableValidation;
    }

    public void setAggregationExpression(String aggregationExpression) {
        this.aggregationExpression = aggregationExpression;
    }

    public void setMethodAgg(boolean methodAgg) {
        this.methodAgg = methodAgg;
    }

    public EPType getOptionalEnumerationType() {
        return optionalEnumerationType;
    }

    public void setOptionalEnumerationType(EPType optionalEnumerationType) {
        this.optionalEnumerationType = optionalEnumerationType;
    }
}
