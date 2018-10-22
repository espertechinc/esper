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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.soda.Expression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapContext;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapper;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.util.SerializerUtil;

import java.util.Collection;
import java.util.List;

public class ExprDeclaredHelper {
    public static Pair<ExprDeclaredNodeImpl, StatementSpecMapContext> getExistsDeclaredExpr(String name,
                                                                                            List<ExprNode> parameters,
                                                                                            Collection<ExpressionDeclItem> stmtLocalExpressions,
                                                                                            ContextCompileTimeDescriptor contextCompileTimeDescriptor,
                                                                                            StatementSpecMapEnv mapEnv) {
        // Find among local expressions
        if (!stmtLocalExpressions.isEmpty()) {
            for (ExpressionDeclItem declNode : stmtLocalExpressions) {
                if (declNode.getName().equals(name)) {
                    Pair<ExprNode, StatementSpecMapContext> pair = getExprDeclaredNode(declNode.getOptionalSoda(), stmtLocalExpressions, contextCompileTimeDescriptor, mapEnv);
                    ExprDeclaredNodeImpl declared = new ExprDeclaredNodeImpl(declNode, parameters, contextCompileTimeDescriptor, pair.getFirst());
                    return new Pair<>(declared, pair.getSecond());
                }
            }
        }

        // find among global expressions
        ExpressionDeclItem found = mapEnv.getExprDeclaredCompileTimeResolver().resolve(name);
        if (found != null) {
            Expression expression = found.getOptionalSoda();
            if (expression == null) {
                byte[] bytes = found.getOptionalSodaBytes().get();
                expression = (Expression) SerializerUtil.byteArrToObject(bytes);
            }
            Pair<ExprNode, StatementSpecMapContext> pair = getExprDeclaredNode(expression, stmtLocalExpressions, contextCompileTimeDescriptor, mapEnv);
            ExprDeclaredNodeImpl declared = new ExprDeclaredNodeImpl(found, parameters, contextCompileTimeDescriptor, pair.getFirst());
            return new Pair<>(declared, pair.getSecond());
        }
        return null;
    }

    private static Pair<ExprNode, StatementSpecMapContext> getExprDeclaredNode(Expression expression, Collection<ExpressionDeclItem> stmtLocalExpressions, ContextCompileTimeDescriptor contextCompileTimeDescriptor, StatementSpecMapEnv mapEnv) {
        StatementSpecMapContext mapContext = new StatementSpecMapContext(contextCompileTimeDescriptor, mapEnv);
        for (ExpressionDeclItem item : stmtLocalExpressions) {
            mapContext.addExpressionDeclarations(item);
        }
        ExprNode body = StatementSpecMapper.mapExpression(expression, mapContext);
        return new Pair<>(body, mapContext);
    }

    public static ExprNodeScript getExistsScript(String defaultDialect, String expressionName, List<ExprNode> parameters, Collection<ExpressionScriptProvided> scriptExpressions, StatementSpecMapEnv mapEnv) {
        if (!scriptExpressions.isEmpty()) {
            ExpressionScriptProvided script = findScript(expressionName, parameters.size(), scriptExpressions);
            if (script != null) {
                return new ExprNodeScript(defaultDialect, script, parameters);
            }
        }

        ExpressionScriptProvided script = mapEnv.getScriptCompileTimeResolver().resolve(expressionName, parameters.size());
        if (script != null) {
            return new ExprNodeScript(defaultDialect, script, parameters);
        }
        return null;
    }

    private static ExpressionScriptProvided findScript(String name, int parameterCount, Collection<ExpressionScriptProvided> scriptsByName) {
        if (scriptsByName == null || scriptsByName.isEmpty()) {
            return null;
        }
        ExpressionScriptProvided nameMatchedScript = null;
        for (ExpressionScriptProvided script : scriptsByName) {
            if (script.getName().equals(name) && script.getParameterNames().length == parameterCount) {
                return script;
            }
            if (script.getName().equals(name)) {
                nameMatchedScript = script;
            }
        }
        return nameMatchedScript;
    }
}
