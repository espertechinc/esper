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
import com.espertech.esper.epl.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.rowregex.RowRegexExprRepeatDesc;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;

import java.util.Locale;
import java.util.Map;

/**
 * Helper class for walking the match-recognize AST.
 */
public class ASTMatchRecognizeHelper {

    private final static String MESSAGE = "Match-recognize AFTER clause must be either AFTER MATCH SKIP TO LAST ROW or AFTER MATCH SKIP TO NEXT ROW or AFTER MATCH SKIP TO CURRENT ROW";

    public static MatchRecognizeSkipEnum parseSkip(CommonTokenStream tokenStream, EsperEPL2GrammarParser.MatchRecogMatchesAfterSkipContext ctx) {

        if ((!ctx.i1.getText().toUpperCase(Locale.ENGLISH).equals("MATCH")) ||
            (!ctx.i2.getText().toUpperCase(Locale.ENGLISH).equals("SKIP")) ||
            (!ctx.i5.getText().toUpperCase(Locale.ENGLISH).equals("ROW"))
            ) {
            throw ASTWalkException.from(MESSAGE, tokenStream, ctx);
        }

        if ((!ctx.i3.getText().toUpperCase(Locale.ENGLISH).equals("TO")) &&
            (!ctx.i3.getText().toUpperCase(Locale.ENGLISH).equals("PAST"))) {
            throw ASTWalkException.from(MESSAGE, tokenStream, ctx);
        }

        if (ctx.i4.getText().toUpperCase(Locale.ENGLISH).equals("LAST")) {
            return MatchRecognizeSkipEnum.PAST_LAST_ROW;
        } else if (ctx.i4.getText().toUpperCase(Locale.ENGLISH).equals("NEXT")) {
            return MatchRecognizeSkipEnum.TO_NEXT_ROW;
        } else if (ctx.i4.getText().toUpperCase(Locale.ENGLISH).equals("CURRENT")) {
            return MatchRecognizeSkipEnum.TO_CURRENT_ROW;
        }
        throw ASTWalkException.from(MESSAGE);
    }

    public static RowRegexExprRepeatDesc walkOptionalRepeat(EsperEPL2GrammarParser.MatchRecogPatternRepeatContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        if (ctx == null) {
            return null;
        }

        ExprNode e1 = ctx.e1 == null ? null : ASTExprHelper.exprCollectSubNodes(ctx.e1, 0, astExprNodeMap).get(0);
        ExprNode e2 = ctx.e2 == null ? null : ASTExprHelper.exprCollectSubNodes(ctx.e2, 0, astExprNodeMap).get(0);

        if (ctx.comma == null && ctx.e1 != null) {
            return new RowRegexExprRepeatDesc(null, null, e1);
        }

        if (e1 == null && e2 == null) {
            throw ASTWalkException.from("Invalid match-recognize quantifier '" + ctx.getText() + "', expecting an expression");
        }

        return new RowRegexExprRepeatDesc(e1, e2, null);
    }
}
