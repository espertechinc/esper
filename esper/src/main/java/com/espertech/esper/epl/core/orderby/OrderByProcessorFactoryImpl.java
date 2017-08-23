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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.OrderByItem;
import com.espertech.esper.util.CollectionUtil;

import java.util.Comparator;
import java.util.List;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorFactoryImpl implements OrderByProcessorFactory {

    private final OrderByElement[] orderBy;
    private final ExprEvaluator[] groupByNodes;
    private final boolean needsGroupByKeys;
    private final Comparator<Object> comparator;

    /**
     * Ctor.
     *
     * @param orderByList         -
     *                            the nodes that generate the keys to sort events on
     * @param groupByEvals        -
     *                            generate the keys for determining aggregation groups
     * @param needsGroupByKeys    -
     *                            indicates whether this processor needs to have individual
     *                            group by keys to evaluate the sort condition successfully
     * @param isSortUsingCollator for string value sorting using compare or Collator
     * @param statementName statement name
     * @param engineImportService engine imports
     * @param onDemandQuery fire-and-forget flag
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when order-by items don't divulge a type
     */
    public OrderByProcessorFactoryImpl(final List<OrderByItem> orderByList,
                                       ExprEvaluator[] groupByEvals,
                                       boolean needsGroupByKeys,
                                       boolean isSortUsingCollator,
                                       EngineImportService engineImportService,
                                       boolean onDemandQuery,
                                       String statementName)
            throws ExprValidationException {
        this.orderBy = toElementArray(orderByList, engineImportService, onDemandQuery, statementName);
        this.groupByNodes = groupByEvals;
        this.needsGroupByKeys = needsGroupByKeys;

        comparator = getComparator(orderBy, isSortUsingCollator);
    }

    public OrderByProcessor instantiate(AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new OrderByProcessorImpl(this, aggregationService);
    }

    public OrderByElement[] getOrderBy() {
        return orderBy;
    }

    public ExprEvaluator[] getGroupByNodes() {
        return groupByNodes;
    }

    public boolean isNeedsGroupByKeys() {
        return needsGroupByKeys;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    /**
     * Returns a comparator for order items that may sort string values using Collator.
     *
     * @param orderBy             order-by items
     * @param isSortUsingCollator true for Collator string sorting
     * @return comparator
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if the return type of order items cannot be determined
     */
    protected static Comparator<Object> getComparator(OrderByElement[] orderBy, boolean isSortUsingCollator) throws ExprValidationException {
        ExprNode[] nodes = new ExprNode[orderBy.length];
        ExprEvaluator[] evaluators = new ExprEvaluator[orderBy.length];
        boolean[] descending = new boolean[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            nodes[i] = orderBy[i].getExprNode();
            evaluators[i] = orderBy[i].getExpr();
            descending[i] = orderBy[i].isDescending();
        }
        return CollectionUtil.getComparator(nodes, evaluators, isSortUsingCollator, descending);
    }

    private OrderByElement[] toElementArray(List<OrderByItem> orderByList, EngineImportService engineImportService, boolean onDemandQuery, String statementName) {
        OrderByElement[] elements = new OrderByElement[orderByList.size()];
        int count = 0;
        for (OrderByItem item : orderByList) {
            ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(item.getExprNode().getForge(), engineImportService, this.getClass(), onDemandQuery, statementName);
            elements[count++] = new OrderByElement(item.getExprNode(), evaluator, item.isDescending());
        }
        return elements;
    }
}
