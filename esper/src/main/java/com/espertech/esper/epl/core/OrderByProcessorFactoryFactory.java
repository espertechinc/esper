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
package com.espertech.esper.epl.core;

import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.OrderByItem;
import com.espertech.esper.epl.spec.RowLimitSpec;
import com.espertech.esper.epl.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.epl.variable.VariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for {@link com.espertech.esper.epl.core.OrderByProcessor} processors.
 */
public class OrderByProcessorFactoryFactory {
    private static final Logger log = LoggerFactory.getLogger(OrderByProcessorFactoryFactory.class);

    /**
     * Returns processor for order-by clauses.
     *
     * @param selectionList       is a list of select expressions
     * @param groupByNodes        is a list of group-by expressions
     * @param orderByList         is a list of order-by expressions
     * @param rowLimitSpec        specification for row limit, or null if no row limit is defined
     * @param variableService     for retrieving variable state for use with row limiting
     * @param isSortUsingCollator for string value sorting using compare or Collator
     * @param optionalContextName context name
     * @return ordering processor instance
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when validation of expressions fails
     */
    public static OrderByProcessorFactory getProcessor(List<SelectClauseExprCompiledSpec> selectionList,
                                                       ExprNode[] groupByNodes,
                                                       List<OrderByItem> orderByList,
                                                       RowLimitSpec rowLimitSpec,
                                                       VariableService variableService,
                                                       boolean isSortUsingCollator,
                                                       String optionalContextName)
            throws ExprValidationException {
        // Get the order by expression nodes
        List<ExprNode> orderByNodes = new ArrayList<ExprNode>();
        for (OrderByItem element : orderByList) {
            orderByNodes.add(element.getExprNode());
        }

        // No order-by clause
        if (orderByList.isEmpty()) {
            log.debug(".getProcessor Using no OrderByProcessor");
            if (rowLimitSpec != null) {
                RowLimitProcessorFactory rowLimitProcessorFactory = new RowLimitProcessorFactory(rowLimitSpec, variableService, optionalContextName);
                return new OrderByProcessorRowLimitOnlyFactory(rowLimitProcessorFactory);
            }
            return null;
        }

        // Determine aggregate functions used in select, if any
        List<ExprAggregateNode> selectAggNodes = new LinkedList<ExprAggregateNode>();
        for (SelectClauseExprCompiledSpec element : selectionList) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(element.getSelectExpression(), selectAggNodes);
        }

        // Get all the aggregate functions occuring in the order-by clause
        List<ExprAggregateNode> orderAggNodes = new LinkedList<ExprAggregateNode>();
        for (ExprNode orderByNode : orderByNodes) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(orderByNode, orderAggNodes);
        }

        validateOrderByAggregates(selectAggNodes, orderAggNodes);

        // Tell the order-by processor whether to compute group-by
        // keys if they are not present
        boolean needsGroupByKeys = !selectionList.isEmpty() && !orderAggNodes.isEmpty();

        log.debug(".getProcessor Using OrderByProcessorImpl");
        OrderByProcessorFactoryImpl orderByProcessorFactory = new OrderByProcessorFactoryImpl(orderByList, groupByNodes, needsGroupByKeys, isSortUsingCollator);
        if (rowLimitSpec == null) {
            return orderByProcessorFactory;
        } else {
            RowLimitProcessorFactory rowLimitProcessorFactory = new RowLimitProcessorFactory(rowLimitSpec, variableService, optionalContextName);
            return new OrderByProcessorOrderedLimitFactory(orderByProcessorFactory, rowLimitProcessorFactory);
        }
    }

    private static void validateOrderByAggregates(List<ExprAggregateNode> selectAggNodes,
                                                  List<ExprAggregateNode> orderAggNodes)
            throws ExprValidationException {
        // Check that the order-by clause doesn't contain
        // any aggregate functions not in the select expression
        for (ExprAggregateNode orderAgg : orderAggNodes) {
            boolean inSelect = false;
            for (ExprAggregateNode selectAgg : selectAggNodes) {
                if (ExprNodeUtility.deepEquals(selectAgg, orderAgg)) {
                    inSelect = true;
                    break;
                }
            }
            if (!inSelect) {
                throw new ExprValidationException("Aggregate functions in the order-by clause must also occur in the select expression");
            }
        }
    }
}
