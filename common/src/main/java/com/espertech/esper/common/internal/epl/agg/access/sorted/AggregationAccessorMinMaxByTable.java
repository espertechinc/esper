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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.REF_ISNEWDATA;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public class AggregationAccessorMinMaxByTable extends AggregationAccessorMinMaxByBase implements AggregationAccessorForge {
    private final TableMetaData table;

    public AggregationAccessorMinMaxByTable(boolean max, TableMetaData table) {
        super(max);
        this.table = table;
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, context.getClassScope(), this.getClass());
        AggregatorAccessSorted forge = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        context.getMethod().getBlock().declareVar(EventBean.class, "event", max ? forge.getLastValueCodegen(context.getClassScope(), context.getMethod()) : forge.getFirstValueCodegen(context.getClassScope(), context.getMethod()))
            .ifRefNullReturnNull("event")
            .methodReturn(exprDotMethod(eventToPublic, "convertToUnd", ref("event"), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }
}