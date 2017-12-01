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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.ops.*;
import com.espertech.esper.epl.join.util.Eligibility;
import com.espertech.esper.epl.join.util.EligibilityDesc;
import com.espertech.esper.epl.join.util.EligibilityUtil;
import com.espertech.esper.epl.join.util.RangeFilterAnalyzer;
import com.espertech.esper.filter.FilterSpecCompilerMakeParamUtil;
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes a filter expression and builds a query graph model.
 * The 'equals', 'and' 'between' and relational operators expressions in the filter expression are extracted
 * and placed in the query graph model as navigable relationships (by key and index
 * properties as well as ranges) between streams.
 */
public class FilterExprAnalyzer {
    /**
     * Analyzes filter expression to build query graph model.
     *
     * @param topNode     - filter top node
     * @param queryGraph  - model containing relationships between streams, to be written to
     * @param isOuterJoin indicator for outer join
     */
    public static void analyze(ExprNode topNode, QueryGraph queryGraph, boolean isOuterJoin) {
        // Analyze relationships between streams. Relationships are properties in AND and EQUALS nodes of joins.
        if (topNode instanceof ExprEqualsNode) {
            ExprEqualsNode equalsNode = (ExprEqualsNode) topNode;
            if (!equalsNode.isNotEquals()) {
                analyzeEqualsNode(equalsNode, queryGraph, isOuterJoin);
            }
        } else if (topNode instanceof ExprAndNode) {
            ExprAndNode andNode = (ExprAndNode) topNode;
            analyzeAndNode(andNode, queryGraph, isOuterJoin);
        } else if (topNode instanceof ExprBetweenNode) {
            ExprBetweenNode betweenNode = (ExprBetweenNode) topNode;
            analyzeBetweenNode(betweenNode, queryGraph);
        } else if (topNode instanceof ExprRelationalOpNode) {
            ExprRelationalOpNode relNode = (ExprRelationalOpNode) topNode;
            analyzeRelationalOpNode(relNode, queryGraph);
        } else if (topNode instanceof FilterExprAnalyzerAffectorProvider) {
            FilterExprAnalyzerAffectorProvider provider = (FilterExprAnalyzerAffectorProvider) topNode;
            analyzeAffectorProvider(provider, queryGraph, isOuterJoin);
        } else if (topNode instanceof ExprInNode) {
            ExprInNode inNode = (ExprInNode) topNode;
            analyzeInNode(inNode, queryGraph);
        } else if (topNode instanceof ExprOrNode) {
            ExprNode rewritten = FilterSpecCompilerMakeParamUtil.rewriteOrToInIfApplicable(topNode);
            if (rewritten instanceof ExprInNode) {
                ExprInNode inNode = (ExprInNode) rewritten;
                analyzeInNode(inNode, queryGraph);
            }
        }
    }

    private static void analyzeInNode(ExprInNode inNode, QueryGraph queryGraph) {
        if (inNode.isNotIn()) {
            return;
        }

        // direction of lookup is value-set (keys) to single-expression (single index)
        analyzeInNodeSingleIndex(inNode, queryGraph);

        // direction of lookup is single-expression (key) to value-set  (multi index)
        analyzeInNodeMultiIndex(inNode, queryGraph);
    }

    private static void analyzeInNodeMultiIndex(ExprInNode inNode, QueryGraph queryGraph) {

        ExprNode[] setExpressions = getInNodeSetExpressions(inNode);
        if (setExpressions.length == 0) {
            return;
        }

        Map<Integer, List<ExprNode>> perStreamExprs = new LinkedHashMap<Integer, List<ExprNode>>();
        for (ExprNode exprNodeSet : setExpressions) {
            if (!(exprNodeSet instanceof ExprIdentNode)) {
                continue;
            }
            ExprIdentNode setIdent = (ExprIdentNode) exprNodeSet;
            addToList(setIdent.getStreamId(), setIdent, perStreamExprs);
        }
        if (perStreamExprs.isEmpty()) {
            return;
        }

        ExprNode testExpr = inNode.getChildNodes()[0];
        Class testExprType = JavaClassHelper.getBoxedType(testExpr.getForge().getEvaluationType());
        if (perStreamExprs.size() > 1) {
            return;
        }
        Map.Entry<Integer, List<ExprNode>> entry = perStreamExprs.entrySet().iterator().next();
        ExprNode[] exprNodes = ExprNodeUtilityCore.toArray(entry.getValue());
        for (ExprNode node : exprNodes) {
            Class exprType = node.getForge().getEvaluationType();
            if (JavaClassHelper.getBoxedType(exprType) != testExprType) {
                return;
            }
        }

        Integer testStreamNum;
        int setStream = entry.getKey();
        if (!(testExpr instanceof ExprIdentNode)) {
            EligibilityDesc eligibility = EligibilityUtil.verifyInputStream(testExpr, setStream);
            if (!eligibility.getEligibility().isEligible()) {
                return;
            }
            if (eligibility.getEligibility() == Eligibility.REQUIRE_ONE && setStream == eligibility.getStreamNum()) {
                return;
            }
            testStreamNum = eligibility.getStreamNum();
        } else {
            testStreamNum = ((ExprIdentNode) testExpr).getStreamId();
        }

        if (testStreamNum == null) {
            queryGraph.addInSetMultiIndexUnkeyed(testExpr, setStream, exprNodes);
        } else {
            if (testStreamNum.equals(entry.getKey())) {
                return;
            }
            queryGraph.addInSetMultiIndex(testStreamNum, testExpr, setStream, exprNodes);
        }
    }

    private static void analyzeInNodeSingleIndex(ExprInNode inNode, QueryGraph queryGraph) {

        if (!(inNode.getChildNodes()[0] instanceof ExprIdentNode)) {
            return;
        }
        ExprIdentNode testIdent = (ExprIdentNode) inNode.getChildNodes()[0];
        Class testIdentClass = JavaClassHelper.getBoxedType(testIdent.getForge().getEvaluationType());
        int indexedStream = testIdent.getStreamId();

        ExprNode[] setExpressions = getInNodeSetExpressions(inNode);
        if (setExpressions.length == 0) {
            return;
        }

        Map<Integer, List<ExprNode>> perStreamExprs = new LinkedHashMap<Integer, List<ExprNode>>();

        for (ExprNode exprNodeSet : setExpressions) {
            if (JavaClassHelper.getBoxedType(exprNodeSet.getForge().getEvaluationType()) != testIdentClass) {
                continue;
            }
            if (exprNodeSet instanceof ExprIdentNode) {
                ExprIdentNode setIdent = (ExprIdentNode) exprNodeSet;
                addToList(setIdent.getStreamId(), setIdent, perStreamExprs);
            } else {
                EligibilityDesc eligibility = EligibilityUtil.verifyInputStream(exprNodeSet, indexedStream);
                if (!eligibility.getEligibility().isEligible()) {
                    continue;
                }
                addToList(eligibility.getStreamNum(), exprNodeSet, perStreamExprs);
            }
        }

        for (Map.Entry<Integer, List<ExprNode>> entry : perStreamExprs.entrySet()) {
            ExprNode[] exprNodes = ExprNodeUtilityCore.toArray(entry.getValue());
            if (entry.getKey() == null) {
                queryGraph.addInSetSingleIndexUnkeyed(testIdent.getStreamId(), testIdent, exprNodes);
                continue;
            }
            if (entry.getKey() != indexedStream) {
                queryGraph.addInSetSingleIndex(testIdent.getStreamId(), testIdent, entry.getKey(), exprNodes);
            }
        }
    }

    private static void addToList(Integer streamIdAllowNull, ExprNode expr, Map<Integer, List<ExprNode>> perStreamExpression) {
        List<ExprNode> perStream = perStreamExpression.get(streamIdAllowNull);
        if (perStream == null) {
            perStream = new ArrayList<ExprNode>();
            perStreamExpression.put(streamIdAllowNull, perStream);
        }
        perStream.add(expr);
    }

    private static ExprNode[] getInNodeSetExpressions(ExprInNode inNode) {
        ExprNode[] setExpressions = new ExprNode[inNode.getChildNodes().length - 1];
        int count = 0;
        for (int i = 1; i < inNode.getChildNodes().length; i++) {
            setExpressions[count++] = inNode.getChildNodes()[i];
        }
        return setExpressions;
    }

    private static void analyzeAffectorProvider(FilterExprAnalyzerAffectorProvider provider, QueryGraph queryGraph, boolean isOuterJoin) {
        FilterExprAnalyzerAffector affector = provider.getAffector(isOuterJoin);
        if (affector == null) {
            return;
        }
        affector.apply(queryGraph);
    }

    private static void analyzeRelationalOpNode(ExprRelationalOpNode relNode, QueryGraph queryGraph) {
        if (((relNode.getChildNodes()[0] instanceof ExprIdentNode)) &&
                ((relNode.getChildNodes()[1] instanceof ExprIdentNode))) {
            ExprIdentNode identNodeLeft = (ExprIdentNode) relNode.getChildNodes()[0];
            ExprIdentNode identNodeRight = (ExprIdentNode) relNode.getChildNodes()[1];

            if (identNodeLeft.getStreamId() != identNodeRight.getStreamId()) {
                queryGraph.addRelationalOpStrict(identNodeLeft.getStreamId(), identNodeLeft,
                        identNodeRight.getStreamId(), identNodeRight, relNode.getRelationalOpEnum());
            }
            return;
        }

        int indexedStream = -1;
        ExprIdentNode indexedPropExpr = null;
        ExprNode exprNodeNoIdent = null;
        RelationalOpEnum relop = relNode.getRelationalOpEnum();

        if (relNode.getChildNodes()[0] instanceof ExprIdentNode) {
            indexedPropExpr = (ExprIdentNode) relNode.getChildNodes()[0];
            indexedStream = indexedPropExpr.getStreamId();
            exprNodeNoIdent = relNode.getChildNodes()[1];
        } else if (relNode.getChildNodes()[1] instanceof ExprIdentNode) {
            indexedPropExpr = (ExprIdentNode) relNode.getChildNodes()[1];
            indexedStream = indexedPropExpr.getStreamId();
            exprNodeNoIdent = relNode.getChildNodes()[0];
            relop = relop.reversed();
        }
        if (indexedStream == -1) {
            return;     // require property of right/left side of equals
        }

        EligibilityDesc eligibility = EligibilityUtil.verifyInputStream(exprNodeNoIdent, indexedStream);
        if (!eligibility.getEligibility().isEligible()) {
            return;
        }

        queryGraph.addRelationalOp(indexedStream, indexedPropExpr, eligibility.getStreamNum(), exprNodeNoIdent, relop);
    }

    private static void analyzeBetweenNode(ExprBetweenNode betweenNode, QueryGraph queryGraph) {
        RangeFilterAnalyzer.apply(betweenNode.getChildNodes()[0], betweenNode.getChildNodes()[1], betweenNode.getChildNodes()[2],
                betweenNode.isLowEndpointIncluded(), betweenNode.isHighEndpointIncluded(), betweenNode.isNotBetween(),
                queryGraph);
    }

    /**
     * Analye EQUALS (=) node.
     *
     * @param equalsNode  - node to analyze
     * @param queryGraph  - store relationships between stream properties
     * @param isOuterJoin indicator for outer join
     */
    protected static void analyzeEqualsNode(ExprEqualsNode equalsNode, QueryGraph queryGraph, boolean isOuterJoin) {
        if ((equalsNode.getChildNodes()[0] instanceof ExprIdentNode) &&
                (equalsNode.getChildNodes()[1] instanceof ExprIdentNode)) {
            ExprIdentNode identNodeLeft = (ExprIdentNode) equalsNode.getChildNodes()[0];
            ExprIdentNode identNodeRight = (ExprIdentNode) equalsNode.getChildNodes()[1];

            if (identNodeLeft.getStreamId() != identNodeRight.getStreamId()) {
                queryGraph.addStrictEquals(identNodeLeft.getStreamId(), identNodeLeft.getResolvedPropertyName(), identNodeLeft,
                        identNodeRight.getStreamId(), identNodeRight.getResolvedPropertyName(), identNodeRight);
            }

            return;
        }
        if (isOuterJoin) {      // outerjoins don't use constants or one-way expression-derived information to evaluate join
            return;
        }

        // handle constant-compare or transformation case
        int indexedStream = -1;
        ExprIdentNode indexedPropExpr = null;
        ExprNode exprNodeNoIdent = null;

        if (equalsNode.getChildNodes()[0] instanceof ExprIdentNode) {
            indexedPropExpr = (ExprIdentNode) equalsNode.getChildNodes()[0];
            indexedStream = indexedPropExpr.getStreamId();
            exprNodeNoIdent = equalsNode.getChildNodes()[1];
        } else if (equalsNode.getChildNodes()[1] instanceof ExprIdentNode) {
            indexedPropExpr = (ExprIdentNode) equalsNode.getChildNodes()[1];
            indexedStream = indexedPropExpr.getStreamId();
            exprNodeNoIdent = equalsNode.getChildNodes()[0];
        }
        if (indexedStream == -1) {
            return;     // require property of right/left side of equals
        }

        EligibilityDesc eligibility = EligibilityUtil.verifyInputStream(exprNodeNoIdent, indexedStream);
        if (!eligibility.getEligibility().isEligible()) {
            return;
        }

        if (eligibility.getEligibility() == Eligibility.REQUIRE_NONE) {
            queryGraph.addUnkeyedExpression(indexedStream, indexedPropExpr, exprNodeNoIdent);
        } else {
            queryGraph.addKeyedExpression(indexedStream, indexedPropExpr, eligibility.getStreamNum(), exprNodeNoIdent);
        }
    }

    /**
     * Analyze the AND-node.
     *
     * @param andNode     - node to analyze
     * @param queryGraph  - to store relationships between stream properties
     * @param isOuterJoin indicator for outer join
     */
    protected static void analyzeAndNode(ExprAndNode andNode, QueryGraph queryGraph, boolean isOuterJoin) {
        for (ExprNode childNode : andNode.getChildNodes()) {
            analyze(childNode, queryGraph, isOuterJoin);
        }
    }
}
