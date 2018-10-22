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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationTableAccessAggReaderForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationTAAReaderSortedMinMaxByForge implements AggregationTableAccessAggReaderForge {
    private final Class resultType;
    private final boolean max;
    private final TableMetaData table;

    public AggregationTAAReaderSortedMinMaxByForge(Class resultType, boolean max, TableMetaData table) {
        this.resultType = resultType;
        this.max = max;
        this.table = table;
    }

    public Class getResultType() {
        return resultType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationTAAReaderSortedMinMaxBy.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationTAAReaderSortedMinMaxBy.class, "strat", newInstance(AggregationTAAReaderSortedMinMaxBy.class))
                .exprDotMethod(ref("strat"), "setMax", constant(max))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
