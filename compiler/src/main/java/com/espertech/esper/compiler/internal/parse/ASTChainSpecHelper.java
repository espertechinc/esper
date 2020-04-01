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

import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableCall;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableName;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.StringValue;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser.ChainableAtomicContext;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser.ChainableWithArgsContext;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ASTChainSpecHelper {

    public static boolean hasChain(EsperEPL2GrammarParser.ChainableElementsContext ctx) {
        return ctx != null && !ctx.chainableAtomicWithOpt().isEmpty();
    }

    public static List<Chainable> getChainables(EsperEPL2GrammarParser.ChainableContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        List<Chainable> chain = new ArrayList<>();

        // handle root
        EsperEPL2GrammarParser.ChainableRootWithOptContext root = ctx.chainableRootWithOpt();
        boolean optionalRoot = root.q != null;
        ChainableWithArgsContext prop = root.chainableWithArgs();
        chain.add(getChainable(prop, optionalRoot, astExprNodeMap));
        addChainablesInternal(ctx.chainableElements(), astExprNodeMap, chain);
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("Empty chain");
        }
        return chain;
    }

    public static List<Chainable> getChainables(EsperEPL2GrammarParser.ChainableElementsContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        List<Chainable> chain = new ArrayList<>();
        addChainablesInternal(ctx, astExprNodeMap, chain);
        return chain;
    }

    public static void addChainablesInternal(EsperEPL2GrammarParser.ChainableElementsContext ctx, Map<Tree, ExprNode> astExprNodeMap, List<Chainable> chain) {
        for (EsperEPL2GrammarParser.ChainableAtomicWithOptContext context : ctx.chainableAtomicWithOpt()) {
            boolean optionalChainable = context.q != null;
            ChainableAtomicContext atomic = context.chainableAtomic();
            Chainable chainable;
            if (atomic.chainableArray() != null) {
                List<ExprNode> params = ASTExprHelper.exprCollectSubNodes(atomic.chainableArray(), 0, astExprNodeMap);
                chainable = new ChainableArray(false, optionalChainable, params);
            } else {
                chainable = getChainable(context.chainableAtomic().chainableWithArgs(), optionalChainable, astExprNodeMap);
            }
            chain.add(chainable);
        }
    }

    private static Chainable getChainable(ChainableWithArgsContext ctx, boolean optional, Map<Tree, ExprNode> astExprNodeMap) {
        boolean distinct = ctx.libFunctionArgs() != null && ctx.libFunctionArgs().DISTINCT() != null;
        String nameUnescaped = ctx.chainableIdent().getText();
        String name = StringValue.removeTicks(nameUnescaped);
        if (ctx.lp == null) {
            return new ChainableName(distinct, optional, name, nameUnescaped);
        }
        List<ExprNode> params = ASTLambdaHelper.getExprNodesLibFunc(ctx.libFunctionArgs(), astExprNodeMap);
        return new ChainableCall(distinct, optional, name, nameUnescaped, params);
    }
}
