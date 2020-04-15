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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapContext;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.dot.walk.ChainableWalkHelper;
import com.espertech.esper.common.internal.util.LazyAllocatedMap;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.tree.Tree;

import java.util.List;
import java.util.Map;

public class ASTChainableHelper {
    public static void processChainable(EsperEPL2GrammarParser.ChainableContext ctx, Map<Tree, ExprNode> astExprNodeMap, ContextCompileTimeDescriptor contextCompileTimeDescriptor, StatementSpecMapEnv mapEnv, StatementSpecRaw statementSpec, ExpressionDeclDesc expressionDeclarations, LazyAllocatedMap<HashableMultiKey, AggregationMultiFunctionForge> plugInAggregations, List<ExpressionScriptProvided> scriptExpressions) {
        // we first convert the event property into chain spec
        List<Chainable> chain = ASTChainSpecHelper.getChainables(ctx, astExprNodeMap);

        // process chain
        StatementSpecMapContext mapContext = new StatementSpecMapContext(contextCompileTimeDescriptor, mapEnv, plugInAggregations, scriptExpressions);
        mapContext.addExpressionDeclarations(expressionDeclarations);
        ExprNode node = ChainableWalkHelper.processDot(false, true, chain, mapContext);
        astExprNodeMap.put(ctx, node);
        mapContext.addTo(statementSpec);
    }
}
