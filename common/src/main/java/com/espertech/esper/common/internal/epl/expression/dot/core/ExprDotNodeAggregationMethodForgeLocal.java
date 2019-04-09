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
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionNode;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprPrecedenceEnum;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeAggregationMethodForgeLocal extends ExprDotNodeAggregationMethodForge {
    private final ExprAggMultiFunctionNode agg;

    public ExprDotNodeAggregationMethodForgeLocal(ExprDotNodeImpl parent, String aggregationMethodName, ExprNode[] parameters, AggregationPortableValidation validation, ExprAggMultiFunctionNode agg) {
        super(parent, aggregationMethodName, parameters, validation);
        this.agg = agg;
    }

    protected CodegenExpression evaluateCodegen(String readerMethodName, Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression future = agg.getAggFuture(classScope);
        CodegenMethod method = parent.makeChild(requiredType, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationRow.class, "row", exprDotMethod(future, "getAggregationRow", exprDotMethod(symbols.getAddExprEvalCtx(parent), "getAgentInstanceId"), symbols.getAddEPS(parent), symbols.getAddIsNewData(parent), symbols.getAddExprEvalCtx(parent)))
            .ifRefNullReturnNull("row")
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(requiredType, exprDotMethod(getReader(classScope), readerMethodName, constant(agg.getColumn()), ref("row"), symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method))));
        return localMethod(method);
    }

    protected void toEPL(StringWriter writer) {
        agg.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    protected String getTableName() {
        return null;
    }

    protected String getTableColumnName() {
        return null;
    }
}
