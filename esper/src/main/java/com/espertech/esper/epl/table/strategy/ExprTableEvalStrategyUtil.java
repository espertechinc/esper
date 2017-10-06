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
package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnPlain;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ExprTableEvalStrategyUtil {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param eventBean bean
     * @return row
     */
    public static AggregationRowPair getRow(ObjectArrayBackedEventBean eventBean) {
        return (AggregationRowPair) eventBean.getProperties()[0];
    }

    protected static Map<String, Object> evalMap(ObjectArrayBackedEventBean event, AggregationRowPair row, Map<String, TableMetadataColumn> items, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        HashMap<String, Object> cols = new HashMap<String, Object>();
        for (Map.Entry<String, TableMetadataColumn> entry : items.entrySet()) {
            if (entry.getValue() instanceof TableMetadataColumnPlain) {
                TableMetadataColumnPlain plain = (TableMetadataColumnPlain) entry.getValue();
                cols.put(entry.getKey(), event.getProperties()[plain.getIndexPlain()]);
            } else {
                TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) entry.getValue();
                if (!aggcol.getFactory().isAccessAggregation()) {
                    cols.put(entry.getKey(), row.getMethods()[aggcol.getMethodOffset()].getValue());
                } else {
                    AggregationAccessorSlotPair pair = aggcol.getAccessAccessorSlotPair();
                    Object value = pair.getAccessor().getValue(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
                    cols.put(entry.getKey(), value);
                }
            }
        }
        return cols;
    }

    protected static Object[] evalTypable(ObjectArrayBackedEventBean event,
                                          AggregationRowPair row,
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
                if (!aggcol.getFactory().isAccessAggregation()) {
                    values[count] = row.getMethods()[aggcol.getMethodOffset()].getValue();
                } else {
                    AggregationAccessorSlotPair pair = aggcol.getAccessAccessorSlotPair();
                    values[count] = pair.getAccessor().getValue(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
                }
            }
            count++;
        }
        return values;
    }

    protected static Object evalAccessorGetValue(AggregationRowPair row, AggregationAccessorSlotPair pair, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        return pair.getAccessor().getValue(row.getStates()[pair.getSlot()], eventsPerStream, newData, context);
    }

    protected static Collection<EventBean> evalGetROCollectionEvents(AggregationRowPair row, AggregationAccessorSlotPair pair, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        return pair.getAccessor().getEnumerableEvents(row.getStates()[pair.getSlot()], eventsPerStream, newData, context);
    }

    protected static EventBean evalGetEventBean(AggregationRowPair row, AggregationAccessorSlotPair pair, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        return pair.getAccessor().getEnumerableEvent(row.getStates()[pair.getSlot()], eventsPerStream, newData, context);
    }

    protected static Collection evalGetROCollectionScalar(AggregationRowPair row, AggregationAccessorSlotPair pair, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        return pair.getAccessor().getEnumerableScalar(row.getStates()[pair.getSlot()], eventsPerStream, newData, context);
    }

    protected static Object evalMethodGetValue(AggregationRowPair row, int index) {
        return row.getMethods()[index].getValue();
    }
}
