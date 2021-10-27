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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ASTFireAndForgetHelper {
    public static List<List<ExprNode>> walkInsertInto(EsperEPL2GrammarParser.FafInsertContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        List<List<ExprNode>> values = new ArrayList<>(ctx.fafInsertRow().size());
        for (EsperEPL2GrammarParser.FafInsertRowContext rowCtx : ctx.fafInsertRow()) {
            values.add(walkInsertIntoRow(rowCtx, astExprNodeMap));
        }
        return values;
    }

    private static List<ExprNode> walkInsertIntoRow(EsperEPL2GrammarParser.FafInsertRowContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        List<ExprNode> result = new ArrayList<>(ctx.expressionList().expression().size());
        for (EsperEPL2GrammarParser.ExpressionContext valueExpr : ctx.expressionList().expression()) {
            ExprNode expr = ASTExprHelper.exprCollectSubNodes(valueExpr, 0, astExprNodeMap).get(0);
            result.add(expr);
        }
        return result;
    }
}
