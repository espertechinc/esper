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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.collection.CombinationEnumeration;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.ops.ExprNotNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprOrNode;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.decomposePopulateConsolidate;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.makeRemainingNode;

public class FilterSpecCompilerIndexPlannerWidthWithConditions {
    protected static FilterSpecPlanForge planRemainingNodesWithConditions(FilterSpecParaForgeMap overallExpressions, FilterSpecCompilerArgs args, int filterServiceMaxFilterWidth, ExprNode topLevelNegator)
            throws ExprValidationException {
        List<ExprNode> unassigned = overallExpressions.getUnassignedExpressions();
        List<ExprOrNode> orNodes = new ArrayList<>(unassigned.size());

        for (ExprNode node : unassigned) {
            if (node instanceof ExprOrNode) {
                orNodes.add((ExprOrNode) node);
            }
        }

        FilterSpecParaForgeMap expressionsWithoutOr = new FilterSpecParaForgeMap();
        expressionsWithoutOr.add(overallExpressions);

        // first dimension: or-node index
        // second dimension: or child node index
        int countOr = 0;
        int sizeFactorized = 1;
        int[] sizePerOr = new int[orNodes.size()];
        OrChildNode[][] orChildNodes = new OrChildNode[orNodes.size()][];
        boolean hasControl = false;
        for (ExprOrNode orNode : orNodes) {
            expressionsWithoutOr.removeNode(orNode);

            // get value-nodes and non-value nodes
            List<ExprNode> nonValueNodes = getNonValueChildNodes(orNode);
            List<ExprNode> valueNodes = new ArrayList<>(Arrays.asList(orNode.getChildNodes()));
            valueNodes.removeAll(nonValueNodes);
            ExprNode singleValueNode = ExprNodeUtilityMake.connectExpressionsByLogicalOrWhenNeeded(valueNodes);

            // get all child nodes; last one is confirm if present
            List<ExprNode> allChildNodes = new ArrayList<>(nonValueNodes);
            if (singleValueNode != null) {
                allChildNodes.add(singleValueNode);
            }

            int len = allChildNodes.size();
            orChildNodes[countOr] = new OrChildNode[len];

            for (int i = 0; i < len; i++) {
                ExprNode child = allChildNodes.get(i);
                if (child == singleValueNode) {
                    hasControl = true;
                    orChildNodes[countOr][i] = new OrChildNodeV(singleValueNode);
                } else {
                    FilterSpecParaForgeMap map = new FilterSpecParaForgeMap();
                    List<ExprNode> nodes = Collections.singletonList(child);
                    ExprNode confirm = decomposePopulateConsolidate(map, true, nodes, args);
                    if (confirm == null) {
                        orChildNodes[countOr][i] = new OrChildNodeNV(child, map);
                    } else {
                        hasControl = true;
                        orChildNodes[countOr][i] = new OrChildNodeNVNegated(child, map, confirm);
                    }
                }
            }

            sizePerOr[countOr] = len;
            sizeFactorized = sizeFactorized * len;
            countOr++;
        }

        // compute permutations
        CombPermutationTriplets[] permutations = new CombPermutationTriplets[sizeFactorized];
        CombinationEnumeration combinationEnumeration = CombinationEnumeration.fromZeroBasedRanges(sizePerOr);
        int count = 0;
        for (; combinationEnumeration.hasMoreElements(); ) {
            Object[] permutation = combinationEnumeration.nextElement();
            permutations[count] = computePermutation(expressionsWithoutOr, permutation, orChildNodes, hasControl, args);
            count++;
        }

        // Remove any permutations that only have a control-confirm
        List<FilterSpecPlanPathForge> result = new ArrayList<>(sizeFactorized);
        List<ExprNode> pathControlConfirm = new ArrayList<>();
        for (CombPermutationTriplets permutation : permutations) {
            if (permutation.getTriplets().length > 0) {
                result.add(new FilterSpecPlanPathForge(permutation.getTriplets(), permutation.getNegateCondition()));
            } else {
                pathControlConfirm.add(permutation.getNegateCondition());
            }
        }

        if (result.size() > filterServiceMaxFilterWidth) {
            return null;
        }

        FilterSpecPlanPathForge[] pathArray = result.toArray(new FilterSpecPlanPathForge[0]);
        ExprNode topLevelConfirmer = ExprNodeUtilityMake.connectExpressionsByLogicalOrWhenNeeded(pathControlConfirm);

        // determine when the path-negate condition is the same as the root confirm-expression
        if (topLevelConfirmer != null) {
            ExprNotNode not = new ExprNotNode();
            not.addChildNode(topLevelConfirmer);
            for (FilterSpecPlanPathForge path : pathArray) {
                if (ExprNodeUtilityCompare.deepEquals(not, path.getPathNegate(), true)) {
                    path.setPathNegate(null);
                }
            }
        }

        MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(args.taggedEventTypes, args.arrayEventTypes, args.allTagNamesOrdered, null, true);
        return new FilterSpecPlanForge(pathArray, topLevelConfirmer, topLevelNegator, convertor);
    }

    private static List<ExprNode> getNonValueChildNodes(ExprOrNode orNode) {
        List<ExprNode> childNodes = new ArrayList<>(orNode.getChildNodes().length);
        for (ExprNode node : orNode.getChildNodes()) {
            FilterSpecExprNodeVisitorValueLimitedExpr visitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
            node.accept(visitor);
            if (!visitor.isLimited()) {
                childNodes.add(node);
            }
        }
        return childNodes;
    }

    private static CombPermutationTriplets computePermutation(FilterSpecParaForgeMap filterParamExprMap, Object[] permutation, OrChildNode[][] orChildNodes, boolean hasControl, FilterSpecCompilerArgs args)
            throws ExprValidationException {
        FilterSpecParaForgeMap mapAll = new FilterSpecParaForgeMap();
        mapAll.add(filterParamExprMap);

        // combine
        List<ExprNode> nvPerOr = new ArrayList<>(permutation.length);
        List<ExprNode> negatingPath = new ArrayList<>(permutation.length);
        for (int orNodeNum = 0; orNodeNum < permutation.length; orNodeNum++) {
            int orChildNodeNum = (Integer) permutation[orNodeNum];
            OrChildNode current = orChildNodes[orNodeNum][orChildNodeNum];
            if (current instanceof OrChildNodeNV) {
                OrChildNodeNV nv = (OrChildNodeNV) current;
                mapAll.add(nv.getMap());
                if (current instanceof OrChildNodeNVNegated) {
                    negatingPath.add(((OrChildNodeNVNegated) current).getControl());
                }
            } else {
                OrChildNodeV v = (OrChildNodeV) current;
                negatingPath.add(v.getNode());
            }

            OrChildNode[] orChildNodesForCurrent = orChildNodes[orNodeNum];
            for (OrChildNode other : orChildNodesForCurrent) {
                if (current == other) {
                    continue;
                }
                if (other instanceof OrChildNodeV) {
                    OrChildNodeV v = (OrChildNodeV) other;
                    ExprNotNode not = new ExprNotNode();
                    not.addChildNode(v.getNode());
                    nvPerOr.add(not);
                }
            }
        }

        // consolidate across
        FilterSpecCompilerConsolidateUtil.consolidate(mapAll, args.statementRawInfo.getStatementName());

        List<FilterSpecPlanPathTripletForge> triplets = new ArrayList<>(mapAll.getTriplets());
        int countUnassigned = mapAll.countUnassignedExpressions();
        if (countUnassigned != 0) {
            FilterSpecPlanPathTripletForge triplet = makeRemainingNode(mapAll.getUnassignedExpressions(), args);
            triplets.add(triplet);
        }

        // without conditions we are done
        FilterSpecPlanPathTripletForge[] tripletsArray = triplets.toArray(new FilterSpecPlanPathTripletForge[0]);
        if (!hasControl) {
            return new CombPermutationTriplets(tripletsArray, null);
        }

        ExprNode negatingNode = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(negatingPath);
        ExprNode excluded = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(nvPerOr);
        ExprNode merged = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(negatingNode, excluded);
        return new CombPermutationTriplets(tripletsArray, merged);
    }

    private static class CombPermutationTriplets {
        private final FilterSpecPlanPathTripletForge[] triplets;
        private final ExprNode negateCondition;

        public CombPermutationTriplets(FilterSpecPlanPathTripletForge[] triplets, ExprNode negateCondition) {
            this.triplets = triplets;
            this.negateCondition = negateCondition;
        }

        public FilterSpecPlanPathTripletForge[] getTriplets() {
            return triplets;
        }

        public ExprNode getNegateCondition() {
            return negateCondition;
        }
    }

    private interface OrChildNode {
    }

    private static class OrChildNodeV implements OrChildNode {
        private final ExprNode node;

        public OrChildNodeV(ExprNode node) {
            this.node = node;
        }

        public ExprNode getNode() {
            return node;
        }
    }

    private static class OrChildNodeNV implements OrChildNode {
        private final ExprNode node;
        private final FilterSpecParaForgeMap map;

        public OrChildNodeNV(ExprNode node, FilterSpecParaForgeMap map) {
            this.node = node;
            this.map = map;
        }

        public ExprNode getNode() {
            return node;
        }

        public FilterSpecParaForgeMap getMap() {
            return map;
        }
    }

    private static class OrChildNodeNVNegated extends OrChildNodeNV {
        private final ExprNode control;

        public OrChildNodeNVNegated(ExprNode node, FilterSpecParaForgeMap map, ExprNode control) {
            super(node, map);
            this.control = control;
        }

        public ExprNode getControl() {
            return control;
        }
    }
}
