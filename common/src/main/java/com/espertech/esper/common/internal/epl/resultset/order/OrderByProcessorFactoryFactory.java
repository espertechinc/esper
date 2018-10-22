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
package com.espertech.esper.common.internal.epl.resultset.order;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator;
import com.espertech.esper.common.internal.compile.stage1.spec.OrderByItem;
import com.espertech.esper.common.internal.compile.stage1.spec.RowLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenFieldSharableComparator.CodegenSharableSerdeName.COMPARATORHASHABLEMULTIKEYS;

/**
 * Factory for {@link OrderByProcessor} processors.
 */
public class OrderByProcessorFactoryFactory {
    private static final Logger log = LoggerFactory.getLogger(OrderByProcessorFactoryFactory.class);

    public static OrderByProcessorFactoryForge getProcessor(List<SelectClauseExprCompiledSpec> selectionList,
                                                            List<OrderByItem> orderByList,
                                                            RowLimitSpec rowLimitSpec,
                                                            VariableCompileTimeResolver variableCompileTimeResolver,
                                                            boolean isSortUsingCollator,
                                                            String optionalContextName,
                                                            OrderByElementForge[][] orderByRollup)
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
                RowLimitProcessorFactoryForge rowLimitProcessorFactory = new RowLimitProcessorFactoryForge(rowLimitSpec, variableCompileTimeResolver, optionalContextName);
                return new OrderByProcessorRowLimitOnlyForge(rowLimitProcessorFactory);
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
        OrderByElementForge[] elements = toElementArray(orderByList);
        CodegenFieldSharable comparator = getComparator(elements, isSortUsingCollator);
        OrderByProcessorForgeImpl orderByProcessorForge = new OrderByProcessorForgeImpl(elements, needsGroupByKeys, orderByRollup, comparator);
        if (rowLimitSpec == null) {
            return orderByProcessorForge;
        } else {
            RowLimitProcessorFactoryForge rowLimitProcessorFactory = new RowLimitProcessorFactoryForge(rowLimitSpec, variableCompileTimeResolver, optionalContextName);
            return new OrderByProcessorOrderedLimitForge(orderByProcessorForge, rowLimitProcessorFactory);
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
                if (ExprNodeUtilityCompare.deepEquals(selectAgg, orderAgg, false)) {
                    inSelect = true;
                    break;
                }
            }
            if (!inSelect) {
                throw new ExprValidationException("Aggregate functions in the order-by clause must also occur in the select expression");
            }
        }
    }

    private static CodegenFieldSharable getComparator(OrderByElementForge[] orderBy, boolean isSortUsingCollator) {
        ExprNode[] nodes = new ExprNode[orderBy.length];
        boolean[] descending = new boolean[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            nodes[i] = orderBy[i].getExprNode();
            descending[i] = orderBy[i].isDescending();
        }
        Class[] types = ExprNodeUtilityQuery.getExprResultTypes(nodes);
        return new CodegenFieldSharableComparator(COMPARATORHASHABLEMULTIKEYS, types, isSortUsingCollator, descending);
    }

    private static OrderByElementForge[] toElementArray(List<OrderByItem> orderByList) {
        OrderByElementForge[] elements = new OrderByElementForge[orderByList.size()];
        int count = 0;
        for (OrderByItem item : orderByList) {
            elements[count++] = new OrderByElementForge(item.getExprNode(), item.isDescending());
        }
        return elements;
    }
}
