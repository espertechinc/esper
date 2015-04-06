/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.table.*;
import com.espertech.esper.epl.table.mgmt.*;

import java.util.concurrent.locks.Lock;

public class ExprTableEvalStrategyFactory {

    public static ExprEvaluator getTableAccessEvalStrategy(ExprNode exprNode, String tableName, Integer streamNum, TableMetadataColumnAggregation agg) {
        if (!agg.getFactory().isAccessAggregation()) {
            return new ExprTableExprEvaluatorMethod(exprNode, tableName, agg.getColumnName(), streamNum, agg.getFactory().getResultType(), agg.getMethodOffset());
        }
        else {
            return new ExprTableExprEvaluatorAccess(exprNode, tableName, agg.getColumnName(), streamNum, agg.getFactory().getResultType(), agg.getAccessAccessorSlotPair(), agg.getOptionalEventType());
        }
    }

    public static ExprTableAccessEvalStrategy getTableAccessEvalStrategy(boolean writesToTables, ExprTableAccessNode tableNode, TableStateInstance state, TableMetadata tableMetadata) {
        ExprEvaluator[] groupKeyEvals = tableNode.getGroupKeyEvaluators();

        TableStateInstanceUngrouped ungrouped;
        TableStateInstanceGroupBy grouped;
        Lock lock;
        if (state instanceof TableStateInstanceUngrouped) {
            ungrouped = (TableStateInstanceUngrouped) state;
            grouped = null;
            lock = writesToTables ? ungrouped.getTableLevelRWLock().writeLock() : ungrouped.getTableLevelRWLock().readLock();
        }
        else {
            grouped = (TableStateInstanceGroupBy) state;
            ungrouped = null;
            lock = writesToTables ? grouped.getTableLevelRWLock().writeLock() : grouped.getTableLevelRWLock().readLock();
        }

        // handle sub-property access
        if (tableNode instanceof ExprTableAccessNodeSubprop) {
            ExprTableAccessNodeSubprop subprop = (ExprTableAccessNodeSubprop) tableNode;
            TableMetadataColumn column = tableMetadata.getTableColumns().get(subprop.getSubpropName());
            return getTableAccessSubprop(lock, subprop, column, grouped, ungrouped);
        }

        // handle top-level access
        if (tableNode instanceof ExprTableAccessNodeTopLevel) {
            if (ungrouped != null) {
                return new ExprTableEvalStrategyUngroupedTopLevel(lock, ungrouped.getEventReference(), tableMetadata.getTableColumns());
            }
            if (tableNode.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByTopLevelMulti(lock, grouped.getRows(), tableMetadata.getTableColumns(), groupKeyEvals);
            }
            return new ExprTableEvalStrategyGroupByTopLevelSingle(lock, grouped.getRows(), tableMetadata.getTableColumns(), groupKeyEvals[0]);
        }

        // handle "keys" function access
        if (tableNode instanceof ExprTableAccessNodeKeys) {
            return new ExprTableEvalStrategyGroupByKeys(lock, grouped.getRows());
        }

        // handle access-aggregator accessors
        if (tableNode instanceof ExprTableAccessNodeSubpropAccessor) {
            ExprTableAccessNodeSubpropAccessor accessorProvider = (ExprTableAccessNodeSubpropAccessor) tableNode;
            TableMetadataColumnAggregation column = (TableMetadataColumnAggregation) tableMetadata.getTableColumns().get(accessorProvider.getSubpropName());
            if (ungrouped != null) {
                AggregationAccessorSlotPair pair = column.getAccessAccessorSlotPair();
                return new ExprTableEvalStrategyUngroupedAccess(lock, ungrouped.getEventReference(), pair.getSlot(), accessorProvider.getAccessor());
            }

            AggregationAccessorSlotPair pair = new AggregationAccessorSlotPair(column.getAccessAccessorSlotPair().getSlot(), accessorProvider.getAccessor());
            if (tableNode.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByAccessMulti(lock, grouped.getRows(), pair, groupKeyEvals);
            }
            return new ExprTableEvalStrategyGroupByAccessSingle(lock, grouped.getRows(), pair, groupKeyEvals[0]);
        }

        throw new IllegalStateException("Unrecognized table access node " + tableNode);
    }

    private static ExprTableAccessEvalStrategy getTableAccessSubprop(Lock lock, ExprTableAccessNodeSubprop subprop, TableMetadataColumn column, TableStateInstanceGroupBy grouped, TableStateInstanceUngrouped ungrouped) {

        if (column instanceof TableMetadataColumnPlain) {
            TableMetadataColumnPlain plain = (TableMetadataColumnPlain) column;
            if (ungrouped != null) {
                return new ExprTableEvalStrategyUngroupedProp(lock, ungrouped.getEventReference(), plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator());
            }
            if (subprop.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByPropMulti(lock, grouped.getRows(), plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator(), subprop.getGroupKeyEvaluators());
            }
            return new ExprTableEvalStrategyGroupByPropSingle(lock, grouped.getRows(), plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator(), subprop.getGroupKeyEvaluators()[0]);
        }

        TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
        if (ungrouped != null) {
            if (!aggcol.getFactory().isAccessAggregation()) {
                return new ExprTableEvalStrategyUngroupedMethod(lock, ungrouped.getEventReference(), aggcol.getMethodOffset());
            }
            AggregationAccessorSlotPair pair = aggcol.getAccessAccessorSlotPair();
            return new ExprTableEvalStrategyUngroupedAccess(lock, ungrouped.getEventReference(), pair.getSlot(), pair.getAccessor());
        }

        TableMetadataColumnAggregation columnAggregation = (TableMetadataColumnAggregation) column;
        if (!columnAggregation.getFactory().isAccessAggregation()) {
            if (subprop.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByMethodMulti(lock, grouped.getRows(), columnAggregation.getMethodOffset(), subprop.getGroupKeyEvaluators());
            }
            return new ExprTableEvalStrategyGroupByMethodSingle(lock, grouped.getRows(), columnAggregation.getMethodOffset(), subprop.getGroupKeyEvaluators()[0]);
        }
        if (subprop.getGroupKeyEvaluators().length > 1) {
            return new ExprTableEvalStrategyGroupByAccessMulti(lock, grouped.getRows(), columnAggregation.getAccessAccessorSlotPair(), subprop.getGroupKeyEvaluators());
        }
        return new ExprTableEvalStrategyGroupByAccessSingle(lock, grouped.getRows(), columnAggregation.getAccessAccessorSlotPair(), subprop.getGroupKeyEvaluators()[0]);
    }
}
