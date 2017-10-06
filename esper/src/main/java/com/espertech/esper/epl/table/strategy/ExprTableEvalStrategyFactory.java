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

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.table.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnPlain;

public class ExprTableEvalStrategyFactory {

    public static ExprTableExprEvaluatorBase getTableAccessEvalStrategy(ExprNode exprNode, String tableName, Integer streamNum, TableMetadataColumnAggregation agg) {
        if (!agg.getFactory().isAccessAggregation()) {
            return new ExprTableExprEvaluatorMethod(exprNode, tableName, agg.getColumnName(), streamNum, agg.getFactory().getResultType(), agg.getMethodOffset());
        } else {
            return new ExprTableExprEvaluatorAccess(exprNode, tableName, agg.getColumnName(), streamNum, agg.getFactory().getResultType(), agg.getAccessAccessorSlotPair(), agg.getOptionalEventType());
        }
    }

    public static ExprTableAccessEvalStrategy getTableAccessEvalStrategy(ExprTableAccessNode tableNode, TableAndLockProvider provider, TableMetadata tableMetadata, boolean isFireAndForget) {
        ExprEvaluator[] groupKeyEvals = tableNode.getGroupKeyEvaluators();

        TableAndLockProviderUngrouped ungrouped;
        TableAndLockProviderGrouped grouped;
        if (provider instanceof TableAndLockProviderUngrouped) {
            ungrouped = (TableAndLockProviderUngrouped) provider;
            grouped = null;
        } else {
            grouped = (TableAndLockProviderGrouped) provider;
            ungrouped = null;
        }

        // handle sub-property access
        if (tableNode instanceof ExprTableAccessNodeSubprop) {
            ExprTableAccessNodeSubprop subprop = (ExprTableAccessNodeSubprop) tableNode;
            TableMetadataColumn column = tableMetadata.getTableColumns().get(subprop.getSubpropName());
            return getTableAccessSubprop(subprop, column, ungrouped, grouped);
        }

        // handle top-level access
        if (tableNode instanceof ExprTableAccessNodeTopLevel) {
            if (ungrouped != null) {
                return new ExprTableEvalStrategyUngroupedTopLevel(ungrouped, tableMetadata.getTableColumns());
            }
            if (tableNode.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByTopLevelMulti(grouped, tableMetadata.getTableColumns(), groupKeyEvals);
            }
            return new ExprTableEvalStrategyGroupByTopLevelSingle(grouped, tableMetadata.getTableColumns(), groupKeyEvals[0]);
        }

        // handle "keys" function access
        if (tableNode instanceof ExprTableAccessNodeKeys) {
            return new ExprTableEvalStrategyGroupByKeys(grouped);
        }

        // handle access-aggregator accessAccessors
        if (tableNode instanceof ExprTableAccessNodeSubpropAccessor) {
            ExprTableAccessNodeSubpropAccessor accessorProvider = (ExprTableAccessNodeSubpropAccessor) tableNode;
            TableMetadataColumnAggregation column = (TableMetadataColumnAggregation) tableMetadata.getTableColumns().get(accessorProvider.getSubpropName());
            if (ungrouped != null) {
                AggregationAccessorSlotPair pair = column.getAccessAccessorSlotPair();
                return new ExprTableEvalStrategyUngroupedAccess(ungrouped, pair.getSlot(), accessorProvider.getAccessor(tableMetadata.getStatementContextCreateTable(), isFireAndForget));
            }

            AggregationAccessorSlotPair pair = new AggregationAccessorSlotPair(column.getAccessAccessorSlotPair().getSlot(), accessorProvider.getAccessor(tableMetadata.getStatementContextCreateTable(), isFireAndForget));
            if (tableNode.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByAccessMulti(grouped, pair, groupKeyEvals);
            }
            return new ExprTableEvalStrategyGroupByAccessSingle(grouped, pair, groupKeyEvals[0]);
        }

        throw new IllegalStateException("Unrecognized table access node " + tableNode);
    }

    private static ExprTableAccessEvalStrategy getTableAccessSubprop(ExprTableAccessNodeSubprop subprop, TableMetadataColumn column, TableAndLockProviderUngrouped ungrouped, TableAndLockProviderGrouped grouped) {

        if (column instanceof TableMetadataColumnPlain) {
            TableMetadataColumnPlain plain = (TableMetadataColumnPlain) column;
            if (ungrouped != null) {
                return new ExprTableEvalStrategyUngroupedProp(ungrouped, plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator());
            }
            if (subprop.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByPropMulti(grouped, plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator(), subprop.getGroupKeyEvaluators());
            }
            return new ExprTableEvalStrategyGroupByPropSingle(grouped, plain.getIndexPlain(), subprop.getOptionalPropertyEnumEvaluator(), subprop.getGroupKeyEvaluators()[0]);
        }

        TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
        if (ungrouped != null) {
            if (!aggcol.getFactory().isAccessAggregation()) {
                return new ExprTableEvalStrategyUngroupedMethod(ungrouped, aggcol.getMethodOffset());
            }
            AggregationAccessorSlotPair pair = aggcol.getAccessAccessorSlotPair();
            return new ExprTableEvalStrategyUngroupedAccess(ungrouped, pair.getSlot(), pair.getAccessor());
        }

        TableMetadataColumnAggregation columnAggregation = (TableMetadataColumnAggregation) column;
        if (!columnAggregation.getFactory().isAccessAggregation()) {
            if (subprop.getGroupKeyEvaluators().length > 1) {
                return new ExprTableEvalStrategyGroupByMethodMulti(grouped, columnAggregation.getMethodOffset(), subprop.getGroupKeyEvaluators());
            }
            return new ExprTableEvalStrategyGroupByMethodSingle(grouped, columnAggregation.getMethodOffset(), subprop.getGroupKeyEvaluators()[0]);
        }
        if (subprop.getGroupKeyEvaluators().length > 1) {
            return new ExprTableEvalStrategyGroupByAccessMulti(grouped, columnAggregation.getAccessAccessorSlotPair(), subprop.getGroupKeyEvaluators());
        }
        return new ExprTableEvalStrategyGroupByAccessSingle(grouped, columnAggregation.getAccessAccessorSlotPair(), subprop.getGroupKeyEvaluators()[0]);
    }
}
