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
package com.espertech.esper.epl.expression.baseagg;

import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.*;

import java.util.*;

public class ExprAggregateNodeUtil {
    public static ExprAggregateNodeParamDesc getValidatePositionalParams(ExprNode[] childNodes, boolean allowOnlyGroupBy)
            throws ExprValidationException {
        ExprAggregateLocalGroupByDesc optionalLocalGroupBy = null;
        int count = 0;
        for (ExprNode node : childNodes) {
            if (!isNonPositionalParameter(node)) {
                count++;
            } else {
                ExprNamedParameterNodeImpl namedParameterNode = (ExprNamedParameterNodeImpl) node;
                if (allowOnlyGroupBy &&
                        !namedParameterNode.getParameterName().toLowerCase(Locale.ENGLISH).equals("group_by")) {
                    throw new ExprValidationException("Invalid named parameter '" + namedParameterNode.getParameterName() + "' (did you mean 'group_by'?)");
                }
                optionalLocalGroupBy = new ExprAggregateLocalGroupByDesc(namedParameterNode.getChildNodes());
            }
        }
        ExprNode[] positionals = new ExprNode[count];
        count = 0;
        for (ExprNode node : childNodes) {
            if (!isNonPositionalParameter(node)) {
                positionals[count++] = node;
            }
        }
        return new ExprAggregateNodeParamDesc(positionals, optionalLocalGroupBy);
    }

    public static boolean isNonPositionalParameter(ExprNode node) {
        return node instanceof ExprNamedParameterNode;
    }

    public static void getAggregatesBottomUp(ExprNode[][] nodes, List<ExprAggregateNode> aggregateNodes) {
        if (nodes == null) {
            return;
        }
        for (ExprNode[] node : nodes) {
            getAggregatesBottomUp(node, aggregateNodes);
        }
    }

    public static void getAggregatesBottomUp(ExprNode[] nodes, List<ExprAggregateNode> aggregateNodes) {
        if (nodes == null) {
            return;
        }
        for (ExprNode node : nodes) {
            getAggregatesBottomUp(node, aggregateNodes);
        }
    }

    /**
     * Populates into the supplied list all aggregation functions within this expression, if any.
     * <p>Populates by going bottom-up such that nested aggregates appear first.
     * <p>I.e. sum(volume * sum(price)) would put first A then B into the list with A=sum(price) and B=sum(volume * A)
     *
     * @param topNode        is the expression node to deep inspect
     * @param aggregateNodes is a list of node to populate into
     */
    public static void getAggregatesBottomUp(ExprNode topNode, List<ExprAggregateNode> aggregateNodes) {
        // Map to hold per level of the node (1 to N depth) of expression node a list of aggregation expr nodes, if any
        // exist at that level
        TreeMap<Integer, List<ExprAggregateNode>> aggregateExprPerLevel = new TreeMap<Integer, List<ExprAggregateNode>>();

        recursiveAggregateHandleSpecial(topNode, aggregateExprPerLevel, 1);

        // Recursively enter all aggregate functions and their level into map
        recursiveAggregateEnter(topNode, aggregateExprPerLevel, 1);

        // Done if none found
        if (aggregateExprPerLevel.isEmpty()) {
            return;
        }

        // From the deepest (highest) level to the lowest, add aggregates to list
        int deepLevel = aggregateExprPerLevel.lastKey();
        for (int i = deepLevel; i >= 1; i--) {
            List<ExprAggregateNode> list = aggregateExprPerLevel.get(i);
            if (list == null) {
                continue;
            }
            aggregateNodes.addAll(list);
        }
    }

    private static void recursiveAggregateHandleSpecial(ExprNode topNode, Map<Integer, List<ExprAggregateNode>> aggregateExprPerLevel, int level) {
        if (topNode instanceof ExprNodeInnerNodeProvider) {
            ExprNodeInnerNodeProvider parameterized = (ExprNodeInnerNodeProvider) topNode;
            List<ExprNode> additionalNodes = parameterized.getAdditionalNodes();
            for (ExprNode additionalNode : additionalNodes) {
                recursiveAggregateEnter(additionalNode, aggregateExprPerLevel, level);
            }
        }

        if (topNode instanceof ExprDeclaredNode) {
            ExprDeclaredNode declared = (ExprDeclaredNode) topNode;
            recursiveAggregateEnter(declared.getBody(), aggregateExprPerLevel, level);
        }
    }

    private static void recursiveAggregateEnter(ExprNode currentNode, Map<Integer, List<ExprAggregateNode>> aggregateExprPerLevel, int currentLevel) {
        // ask all child nodes to enter themselves
        for (ExprNode node : currentNode.getChildNodes()) {
            recursiveAggregateHandleSpecial(node, aggregateExprPerLevel, currentLevel + 1);

            recursiveAggregateEnter(node, aggregateExprPerLevel, currentLevel + 1);
        }

        if (!(currentNode instanceof ExprAggregateNode)) {
            return;
        }

        // Add myself to list, I'm an aggregate function
        List<ExprAggregateNode> aggregates = aggregateExprPerLevel.get(currentLevel);
        if (aggregates == null) {
            aggregates = new LinkedList<ExprAggregateNode>();
            aggregateExprPerLevel.put(currentLevel, aggregates);
        }
        aggregates.add((ExprAggregateNode) currentNode);
    }

    public static int countPositionalArgs(List<ExprNode> args) {
        int count = 0;
        for (ExprNode expr : args) {
            if (!isNonPositionalParameter(expr)) {
                count++;
            }
        }
        return count;
    }
}
