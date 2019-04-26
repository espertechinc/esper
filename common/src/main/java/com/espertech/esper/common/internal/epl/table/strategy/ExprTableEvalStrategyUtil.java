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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnPlain;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprTableEvalStrategyUtil {
    public static AggregationRow getRow(ObjectArrayBackedEventBean eventBean) {
        return getRow(eventBean.getProperties());
    }

    public static AggregationRow getRow(Object[] underlying) {
        return (AggregationRow) underlying[0];
    }

    public static CodegenExpression codegenInitMap(Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses, Class generator, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Map.class, generator, classScope);
        method.getBlock()
                .declareVar(Map.class, "ta", newInstance(LinkedHashMap.class, constant(tableAccesses.size() + 2)));
        for (Map.Entry<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> entry : tableAccesses.entrySet()) {
            method.getBlock().exprDotMethod(ref("ta"), "put", constant(entry.getKey().getTableAccessNumber()), entry.getValue().make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("ta"));
        return localMethod(method);
    }

    protected static Map<String, Object> evalMap(ObjectArrayBackedEventBean event, AggregationRow row, Map<String, TableMetadataColumn> items, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        HashMap<String, Object> cols = new HashMap<String, Object>();
        for (Map.Entry<String, TableMetadataColumn> entry : items.entrySet()) {
            if (entry.getValue() instanceof TableMetadataColumnPlain) {
                TableMetadataColumnPlain plain = (TableMetadataColumnPlain) entry.getValue();
                cols.put(entry.getKey(), event.getProperties()[plain.getIndexPlain()]);
            } else {
                TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) entry.getValue();
                cols.put(entry.getKey(), row.getValue(aggcol.getColumn(), eventsPerStream, isNewData, exprEvaluatorContext));
            }
        }
        return cols;
    }

    protected static Object[] evalTypable(ObjectArrayBackedEventBean event,
                                          AggregationRow row,
                                          Map<String, TableMetadataColumn> items,
                                          EventBean[] eventsPerStream,
                                          boolean isNewData,
                                          ExprEvaluatorContext exprEvaluatorContext) {
        Object[] values = new Object[items.size()];
        int count = 0;
        for (Map.Entry<String, TableMetadataColumn> entry : items.entrySet()) {
            if (entry.getValue() instanceof TableMetadataColumnPlain) {
                TableMetadataColumnPlain plain = (TableMetadataColumnPlain) entry.getValue();
                values[count] = event.getProperties()[plain.getIndexPlain()];
            } else {
                TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) entry.getValue();
                values[count] = row.getValue(aggcol.getColumn(), eventsPerStream, isNewData, exprEvaluatorContext);
            }
            count++;
        }
        return values;
    }
}
