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

import com.espertech.esper.common.internal.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ASTLambdaHelper {
    public static List<ExprNode> getExprNodesLibFunc(EsperEPL2GrammarParser.LibFunctionArgsContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        List<EsperEPL2GrammarParser.LibFunctionArgItemContext> args = ctx.libFunctionArgItem();
        if (args == null || args.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExprNode> parameters = new ArrayList<>(args.size());
        for (EsperEPL2GrammarParser.LibFunctionArgItemContext arg : args) {
            if (arg.expressionLambdaDecl() != null) {
                List<String> lambdaparams = getLambdaGoesParams(arg.expressionLambdaDecl());
                ExprLambdaGoesNode goes = new ExprLambdaGoesNode(lambdaparams);
                ExprNode lambdaExpr = ASTExprHelper.exprCollectSubNodes(arg.expressionWithNamed(), 0, astExprNodeMap).get(0);
                goes.addChildNode(lambdaExpr);
                parameters.add(goes);
            } else {
                List<ExprNode> params = ASTExprHelper.exprCollectSubNodes(arg.expressionWithNamed(), 0, astExprNodeMap);
                ExprNode parameter = params.get(0);
                parameters.add(parameter);
            }
        }
        return parameters;
    }

    protected static List<String> getLambdaGoesParams(EsperEPL2GrammarParser.ExpressionLambdaDeclContext ctx) {
        List<String> parameters;
        if (ctx.i != null) {
            parameters = new ArrayList<String>(1);
            parameters.add(ctx.i.getText());
        } else {
            parameters = ASTUtil.getIdentList(ctx.columnListKeywordAllowed());
        }
        return parameters;
    }
}
