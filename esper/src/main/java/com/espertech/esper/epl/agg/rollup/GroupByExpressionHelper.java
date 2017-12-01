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
package com.espertech.esper.epl.agg.rollup;

import com.espertech.esper.collection.CombinationEnumeration;
import com.espertech.esper.collection.MultiKeyInt;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.SerializableObjectCopier;

import java.io.StringWriter;
import java.util.*;

public class GroupByExpressionHelper {
    public static GroupByClauseExpressions getGroupByRollupExpressions(List<GroupByClauseElement> groupByElements,
                                                                       SelectClauseSpecRaw selectClauseSpec, ExprNode optionalHavingNode, List<OrderByItem> orderByList,
                                                                       ExprNodeSubselectDeclaredDotVisitor visitor)
            throws ExprValidationException {
        if (groupByElements == null || groupByElements.size() == 0) {
            return null;
        }

        // walk group-by-elements, determine group-by expressions and rollup nodes
        GroupByExpressionInfo groupByExpressionInfo = groupByToRollupNodes(groupByElements);

        // obtain expression nodes, collect unique nodes and assign index
        List<ExprNode> distinctGroupByExpressions = new ArrayList<ExprNode>();
        Map<ExprNode, Integer> expressionToIndex = new HashMap<ExprNode, Integer>();
        for (ExprNode exprNode : groupByExpressionInfo.getExpressions()) {
            boolean found = false;
            for (int i = 0; i < distinctGroupByExpressions.size(); i++) {
                ExprNode other = distinctGroupByExpressions.get(i);
                // find same expression
                if (ExprNodeUtilityCore.deepEquals(exprNode, other, false)) {
                    expressionToIndex.put(exprNode, i);
                    found = true;
                    break;
                }
            }

            // not seen before
            if (!found) {
                expressionToIndex.put(exprNode, distinctGroupByExpressions.size());
                distinctGroupByExpressions.add(exprNode);
            }
        }

        // determine rollup, validate it is either (not both)
        boolean hasGroupingSet = false;
        boolean hasRollup = false;
        for (GroupByClauseElement element : groupByElements) {
            if (element instanceof GroupByClauseElementGroupingSet) {
                hasGroupingSet = true;
            }
            if (element instanceof GroupByClauseElementRollupOrCube) {
                hasRollup = true;
            }
        }

        // no-rollup or grouping-sets means simply validate
        ExprNode[] groupByExpressions = distinctGroupByExpressions.toArray(new ExprNode[distinctGroupByExpressions.size()]);
        if (!hasRollup && !hasGroupingSet) {
            return new GroupByClauseExpressions(groupByExpressions);
        }

        // evaluate rollup node roots
        List<GroupByRollupNodeBase> nodes = groupByExpressionInfo.getNodes();
        Object[][] perNodeCombinations = new Object[nodes.size()][];
        GroupByRollupEvalContext context = new GroupByRollupEvalContext(expressionToIndex);
        try {
            for (int i = 0; i < nodes.size(); i++) {
                GroupByRollupNodeBase node = nodes.get(i);
                List<int[]> combinations = node.evaluate(context);
                perNodeCombinations[i] = new Object[combinations.size()];
                for (int j = 0; j < combinations.size(); j++) {
                    perNodeCombinations[i][j] = combinations.get(j);
                }
            }
        } catch (GroupByRollupDuplicateException ex) {
            if (ex.getIndexes().length == 0) {
                throw new ExprValidationException("Failed to validate the group-by clause, found duplicate specification of the overall grouping '()'");
            } else {
                StringWriter writer = new StringWriter();
                String delimiter = "";
                for (int i = 0; i < ex.getIndexes().length; i++) {
                    writer.append(delimiter);
                    writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(groupByExpressions[ex.getIndexes()[i]]));
                    delimiter = ", ";
                }
                throw new ExprValidationException("Failed to validate the group-by clause, found duplicate specification of expressions (" + writer.toString() + ")");
            }
        }

        // enumerate combinations building an index list
        CombinationEnumeration combinationEnumeration = new CombinationEnumeration(perNodeCombinations);
        Set<Integer> combination = new TreeSet<Integer>();
        Set<MultiKeyInt> indexList = new LinkedHashSet<MultiKeyInt>();
        for (; combinationEnumeration.hasMoreElements(); ) {
            combination.clear();
            Object[] combinationOA = combinationEnumeration.nextElement();
            for (Object indexes : combinationOA) {
                int[] indexarr = (int[]) indexes;
                for (int anIndex : indexarr) {
                    combination.add(anIndex);
                }
            }
            int[] indexArr = CollectionUtil.intArray(combination);
            indexList.add(new MultiKeyInt(indexArr));
        }

        // obtain rollup levels
        int[][] rollupLevels = new int[indexList.size()][];
        int count = 0;
        for (MultiKeyInt mk : indexList) {
            rollupLevels[count++] = mk.getKeys();
        }
        int numberOfLevels = rollupLevels.length;
        if (numberOfLevels == 1 && rollupLevels[0].length == 0) {
            throw new ExprValidationException("Failed to validate the group-by clause, the overall grouping '()' cannot be the only grouping");
        }

        // obtain select-expression copies for rewrite
        List<SelectClauseElementRaw> expressions = selectClauseSpec.getSelectExprList();
        ExprNode[][] selects = new ExprNode[numberOfLevels][];
        for (int i = 0; i < numberOfLevels; i++) {
            selects[i] = new ExprNode[expressions.size()];
            for (int j = 0; j < expressions.size(); j++) {
                SelectClauseElementRaw selectRaw = expressions.get(j);
                if (!(selectRaw instanceof SelectClauseExprRawSpec)) {
                    throw new ExprValidationException("Group-by with rollup requires that the select-clause does not use wildcard");
                }
                SelectClauseExprRawSpec compiled = (SelectClauseExprRawSpec) selectRaw;
                selects[i][j] = copyVisitExpression(compiled.getSelectExpression(), visitor);
            }
        }

        // obtain having-expression copies for rewrite
        ExprNode[] optHavingNodeCopy = null;
        if (optionalHavingNode != null) {
            optHavingNodeCopy = new ExprNode[numberOfLevels];
            for (int i = 0; i < numberOfLevels; i++) {
                optHavingNodeCopy[i] = copyVisitExpression(optionalHavingNode, visitor);
            }
        }

        // obtain orderby-expression copies for rewrite
        ExprNode[][] optOrderByCopy = null;
        if (orderByList != null && orderByList.size() > 0) {
            optOrderByCopy = new ExprNode[numberOfLevels][];
            for (int i = 0; i < numberOfLevels; i++) {
                optOrderByCopy[i] = new ExprNode[orderByList.size()];
                for (int j = 0; j < orderByList.size(); j++) {
                    OrderByItem element = orderByList.get(j);
                    optOrderByCopy[i][j] = copyVisitExpression(element.getExprNode(), visitor);
                }
            }
        }

        return new GroupByClauseExpressions(groupByExpressions, rollupLevels, selects, optHavingNodeCopy, optOrderByCopy);
    }

    private static GroupByExpressionInfo groupByToRollupNodes(List<GroupByClauseElement> groupByExpressions) {
        List<GroupByRollupNodeBase> parents = new ArrayList<GroupByRollupNodeBase>(groupByExpressions.size());
        List<ExprNode> exprNodes = new ArrayList<ExprNode>();

        for (GroupByClauseElement element : groupByExpressions) {

            GroupByRollupNodeBase parent;
            if (element instanceof GroupByClauseElementExpr) {
                GroupByClauseElementExpr expr = (GroupByClauseElementExpr) element;
                exprNodes.add(expr.getExpr());
                parent = new GroupByRollupNodeSingleExpr(expr.getExpr());
            } else if (element instanceof GroupByClauseElementRollupOrCube) {
                GroupByClauseElementRollupOrCube spec = (GroupByClauseElementRollupOrCube) element;
                parent = new GroupByRollupNodeRollupOrCube(spec.isCube());
                groupByAddRollup(spec, parent, exprNodes);
            } else if (element instanceof GroupByClauseElementGroupingSet) {
                GroupByClauseElementGroupingSet spec = (GroupByClauseElementGroupingSet) element;
                parent = new GroupByRollupNodeGroupingSet();
                for (GroupByClauseElement groupElement : spec.getElements()) {
                    if (groupElement instanceof GroupByClauseElementExpr) {
                        GroupByClauseElementExpr single = (GroupByClauseElementExpr) groupElement;
                        exprNodes.add(single.getExpr());
                        parent.add(new GroupByRollupNodeSingleExpr(single.getExpr()));
                    }
                    if (groupElement instanceof GroupByClauseElementCombinedExpr) {
                        GroupByClauseElementCombinedExpr combined = (GroupByClauseElementCombinedExpr) groupElement;
                        exprNodes.addAll(combined.getExpressions());
                        parent.add(new GroupByRollupNodeCombinedExpr(combined.getExpressions()));
                    }
                    if (groupElement instanceof GroupByClauseElementRollupOrCube) {
                        GroupByClauseElementRollupOrCube rollup = (GroupByClauseElementRollupOrCube) groupElement;
                        GroupByRollupNodeRollupOrCube node = new GroupByRollupNodeRollupOrCube(rollup.isCube());
                        groupByAddRollup(rollup, node, exprNodes);
                        parent.add(node);
                    }
                }
            } else {
                throw new IllegalStateException("Unexpected group-by clause element " + element);
            }
            parents.add(parent);
        }

        return new GroupByExpressionInfo(exprNodes, parents);
    }

    private static void groupByAddRollup(GroupByClauseElementRollupOrCube spec, GroupByRollupNodeBase parent, List<ExprNode> exprNodes) {
        for (GroupByClauseElement rolledUp : spec.getRollupExpressions()) {
            if (rolledUp instanceof GroupByClauseElementExpr) {
                GroupByClauseElementExpr expr = (GroupByClauseElementExpr) rolledUp;
                exprNodes.add(expr.getExpr());
                parent.add(new GroupByRollupNodeSingleExpr(expr.getExpr()));
            } else {
                GroupByClauseElementCombinedExpr combined = (GroupByClauseElementCombinedExpr) rolledUp;
                exprNodes.addAll(combined.getExpressions());
                parent.add(new GroupByRollupNodeCombinedExpr(combined.getExpressions()));
            }
        }
    }

    private static ExprNode copyVisitExpression(ExprNode expression, ExprNodeSubselectDeclaredDotVisitor visitor) {
        try {
            ExprNode node = (ExprNode) SerializableObjectCopier.copy(expression);
            node.accept(visitor);
            return node;
        } catch (Exception e) {
            throw new RuntimeException("Internal error providing expression tree: " + e.getMessage(), e);
        }
    }

    private static class GroupByExpressionInfo {
        private final List<ExprNode> expressions;
        private final List<GroupByRollupNodeBase> nodes;

        private GroupByExpressionInfo(List<ExprNode> expressions, List<GroupByRollupNodeBase> nodes) {
            this.expressions = expressions;
            this.nodes = nodes;
        }

        public List<ExprNode> getExpressions() {
            return expressions;
        }

        public List<GroupByRollupNodeBase> getNodes() {
            return nodes;
        }
    }
}
