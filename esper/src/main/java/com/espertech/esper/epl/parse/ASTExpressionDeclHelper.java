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

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ASTExpressionDeclHelper {
    public static Pair<ExpressionDeclItem, ExpressionScriptProvided> walkExpressionDecl(EsperEPL2GrammarParser.ExpressionDeclContext ctx, List<String> scriptBodies, Map<Tree, ExprNode> astExprNodeMap, CommonTokenStream tokenStream) {

        String name = ctx.name.getText();

        if (ctx.alias != null) {
            if (!ctx.alias.getText().toLowerCase(Locale.ENGLISH).trim().equals("alias")) {
                throw ASTWalkException.from("For expression alias '" + name + "' expecting 'alias' keyword but received '" + ctx.alias.getText() + "'");
            }
            if (ctx.columnList() != null) {
                throw ASTWalkException.from("For expression alias '" + name + "' expecting no parameters but received '" + tokenStream.getText(ctx.columnList()) + "'");
            }
            if (ctx.expressionDef() != null && ctx.expressionDef().expressionLambdaDecl() != null) {
                throw ASTWalkException.from("For expression alias '" + name + "' expecting an expression without parameters but received '" + tokenStream.getText(ctx.expressionDef().expressionLambdaDecl()) + "'");
            }
            if (ctx.expressionDef().stringconstant() != null) {
                throw ASTWalkException.from("For expression alias '" + name + "' expecting an expression but received a script");
            }
            ExprNode node = ASTExprHelper.exprCollectSubNodes(ctx, 0, astExprNodeMap).get(0);
            String alias = ctx.name.getText();
            ExpressionDeclItem decl = new ExpressionDeclItem(alias, Collections.<String>emptyList(), node, true);
            return new Pair<ExpressionDeclItem, ExpressionScriptProvided>(decl, null);
        }

        if (ctx.expressionDef().stringconstant() != null) {
            String expressionText = scriptBodies.remove(0);
            List<String> parameters = ASTUtil.getIdentList(ctx.columnList());
            String optionalReturnType = ctx.classIdentifier() == null ? null : ASTUtil.unescapeClassIdent(ctx.classIdentifier());
            boolean optionalReturnTypeArray = ctx.array != null;
            String optionalDialect = ctx.expressionDialect() == null ? null : ctx.expressionDialect().d.getText();
            String optionalEventTypeName = ASTTypeExpressionAnnoHelper.expectMayTypeAnno(ctx.typeExpressionAnnotation(), tokenStream);
            ExpressionScriptProvided script = new ExpressionScriptProvided(name, expressionText, parameters,
                    optionalReturnType, optionalReturnTypeArray, optionalEventTypeName, optionalDialect);
            return new Pair<ExpressionDeclItem, ExpressionScriptProvided>(null, script);
        }

        EsperEPL2GrammarParser.ExpressionDefContext ctxexpr = ctx.expressionDef();
        ExprNode inner = ASTExprHelper.exprCollectSubNodes(ctxexpr.expression(), 0, astExprNodeMap).get(0);

        List<String> parametersNames = Collections.emptyList();
        EsperEPL2GrammarParser.ExpressionLambdaDeclContext lambdactx = ctxexpr.expressionLambdaDecl();
        if (ctxexpr.expressionLambdaDecl() != null) {
            parametersNames = ASTLibFunctionHelper.getLambdaGoesParams(lambdactx);
        }

        ExpressionDeclItem expr = new ExpressionDeclItem(name, parametersNames, inner, false);
        return new Pair<ExpressionDeclItem, ExpressionScriptProvided>(expr, null);
    }
}
