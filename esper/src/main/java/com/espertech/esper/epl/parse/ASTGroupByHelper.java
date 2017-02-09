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
package com.espertech.esper.epl.parse;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.*;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ASTGroupByHelper {
    public static void walkGroupBy(EsperEPL2GrammarParser.GroupByListExprContext ctx, Map<Tree, ExprNode> astExprNodeMap, List<GroupByClauseElement> groupByExpressions) {
        List<EsperEPL2GrammarParser.GroupByListChoiceContext> choices = ctx.groupByListChoice();
        for (EsperEPL2GrammarParser.GroupByListChoiceContext choice : choices) {
            GroupByClauseElement element = walkChoice(choice, astExprNodeMap);
            groupByExpressions.add(element);
        }
    }

    private static GroupByClauseElement walkChoice(EsperEPL2GrammarParser.GroupByListChoiceContext choice, Map<Tree, ExprNode> astExprNodeMap) {
        if (choice.e1 != null) {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(choice.e1, 0, astExprNodeMap).get(0);
            return new GroupByClauseElementExpr(expr);
        }
        if (choice.groupByCubeOrRollup() != null) {
            return walkCubeOrRollup(choice.groupByCubeOrRollup(), astExprNodeMap);
        }
        return walkGroupingSets(choice.groupByGroupingSets().groupBySetsChoice(), astExprNodeMap);

    }

    private static GroupByClauseElement walkCubeOrRollup(EsperEPL2GrammarParser.GroupByCubeOrRollupContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        boolean cube = ctx.CUBE() != null;
        List<GroupByClauseElement> combinables = walkCombinables(ctx.groupByCombinableExpr(), astExprNodeMap);
        return new GroupByClauseElementRollupOrCube(cube, combinables);
    }

    private static List<GroupByClauseElement> walkCombinables(List<EsperEPL2GrammarParser.GroupByCombinableExprContext> ctxs, Map<Tree, ExprNode> astExprNodeMap) {
        List<GroupByClauseElement> elements = new ArrayList<GroupByClauseElement>();
        for (EsperEPL2GrammarParser.GroupByCombinableExprContext ctx : ctxs) {
            elements.add(walkCombinable(ctx, astExprNodeMap));
        }
        return elements;
    }

    private static GroupByClauseElement walkCombinable(EsperEPL2GrammarParser.GroupByCombinableExprContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        if (ctx.e1 != null && ctx.LPAREN() == null) {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(ctx.e1, 0, astExprNodeMap).get(0);
            return new GroupByClauseElementExpr(expr);
        }
        List<ExprNode> combined = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap);
        return new GroupByClauseElementCombinedExpr(combined);
    }

    private static GroupByClauseElementGroupingSet walkGroupingSets(List<EsperEPL2GrammarParser.GroupBySetsChoiceContext> ctxs, Map<Tree, ExprNode> astExprNodeMap) {
        List<GroupByClauseElement> elements = new ArrayList<GroupByClauseElement>();
        for (EsperEPL2GrammarParser.GroupBySetsChoiceContext ctx : ctxs) {
            if (ctx.groupByCubeOrRollup() != null) {
                elements.add(walkCubeOrRollup(ctx.groupByCubeOrRollup(), astExprNodeMap));
            } else {
                elements.add(walkCombinable(ctx.groupByCombinableExpr(), astExprNodeMap));
            }
        }
        return new GroupByClauseElementGroupingSet(elements);
    }
}
