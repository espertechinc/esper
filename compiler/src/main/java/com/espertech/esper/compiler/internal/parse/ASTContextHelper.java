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

import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ASTContextHelper {
    public static CreateContextDesc walkCreateContext(EsperEPL2GrammarParser.CreateContextExprContext ctx, Map<Tree, ExprNode> astExprNodeMap, Map<Tree, EvalForgeNode> astPatternNodeMap, PropertyEvalSpec propertyEvalSpec, FilterSpecRaw filterSpec) {
        String contextName = ctx.name.getText();
        ContextSpec contextDetail;

        EsperEPL2GrammarParser.CreateContextChoiceContext choice = ctx.createContextDetail().createContextChoice();
        if (choice != null) {
            contextDetail = walkChoice(choice, astExprNodeMap, astPatternNodeMap, propertyEvalSpec);
        } else {
            contextDetail = walkNested(ctx.createContextDetail().contextContextNested(), astExprNodeMap, astPatternNodeMap, propertyEvalSpec, filterSpec);
        }
        return new CreateContextDesc(contextName, contextDetail);
    }

    private static ContextSpec walkNested(List<EsperEPL2GrammarParser.ContextContextNestedContext> nestedContexts, Map<Tree, ExprNode> astExprNodeMap, Map<Tree, EvalForgeNode> astPatternNodeMap, PropertyEvalSpec propertyEvalSpec, FilterSpecRaw filterSpec) {
        List<CreateContextDesc> contexts = new ArrayList<CreateContextDesc>(nestedContexts.size());
        for (EsperEPL2GrammarParser.ContextContextNestedContext nestedctx : nestedContexts) {
            ContextSpec contextDetail = walkChoice(nestedctx.createContextChoice(), astExprNodeMap, astPatternNodeMap, propertyEvalSpec);
            CreateContextDesc desc = new CreateContextDesc(nestedctx.name.getText(), contextDetail);
            contexts.add(desc);
        }
        return new ContextNested(contexts);
    }

    private static ContextSpec walkChoice(EsperEPL2GrammarParser.CreateContextChoiceContext ctx, Map<Tree, ExprNode> astExprNodeMap, Map<Tree, EvalForgeNode> astPatternNodeMap, PropertyEvalSpec propertyEvalSpec) {

        // temporal fixed (start+end) and overlapping (initiated/terminated)
        if (ctx.START() != null || ctx.INITIATED() != null) {

            ExprNode[] distinctExpressions = null;
            if (ctx.createContextDistinct() != null) {
                if (ctx.createContextDistinct().expressionList() == null) {
                    distinctExpressions = ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY;
                } else {
                    distinctExpressions = ASTExprHelper.exprCollectSubNodesPerNode(ctx.createContextDistinct().expressionList().expression(), astExprNodeMap);
                }
            }

            ContextSpecCondition startEndpoint;
            if (ctx.START() != null) {
                boolean immediate = checkNow(ctx.i);
                if (immediate) {
                    startEndpoint = ContextSpecConditionImmediate.INSTANCE;
                } else {
                    startEndpoint = getContextCondition(ctx.r1, astExprNodeMap, astPatternNodeMap, propertyEvalSpec, false);
                }
            } else {
                boolean immediate = checkNow(ctx.i);
                startEndpoint = getContextCondition(ctx.r1, astExprNodeMap, astPatternNodeMap, propertyEvalSpec, immediate);
            }

            boolean overlapping = ctx.INITIATED() != null;
            ContextSpecCondition endEndpoint = getContextCondition(ctx.r2, astExprNodeMap, astPatternNodeMap, propertyEvalSpec, false);
            return new ContextSpecInitiatedTerminated(startEndpoint, endEndpoint, overlapping, distinctExpressions);
        }

        // partitioned
        if (ctx.PARTITION() != null) {
            List<EsperEPL2GrammarParser.CreateContextPartitionItemContext> partitions = ctx.createContextPartitionItem();
            List<ContextSpecKeyedItem> rawSpecs = new ArrayList<ContextSpecKeyedItem>();
            for (EsperEPL2GrammarParser.CreateContextPartitionItemContext partition : partitions) {

                FilterSpecRaw filterSpec = ASTFilterSpecHelper.walkFilterSpec(partition.eventFilterExpression(), propertyEvalSpec, astExprNodeMap);
                propertyEvalSpec = null;

                List<String> propertyNames = new ArrayList<String>();
                List<EsperEPL2GrammarParser.EventPropertyContext> properties = partition.eventProperty();
                for (EsperEPL2GrammarParser.EventPropertyContext property : properties) {
                    String propertyName = ASTUtil.getPropertyName(property, 0);
                    propertyNames.add(propertyName);
                }
                ASTExprHelper.exprCollectSubNodes(partition, 0, astExprNodeMap); // remove expressions

                rawSpecs.add(new ContextSpecKeyedItem(filterSpec, propertyNames, partition.keywordAllowedIdent() == null ? null : partition.keywordAllowedIdent().getText()));
            }

            List<ContextSpecConditionFilter> optionalInit = null;
            if (ctx.createContextPartitionInit() != null) {
                optionalInit = getContextPartitionInit(ctx.createContextPartitionInit().createContextFilter(), astExprNodeMap);
            }

            ContextSpecCondition optionalTermination = null;
            if (ctx.createContextPartitionTerm() != null) {
                optionalTermination = getContextCondition(ctx.createContextPartitionTerm().createContextRangePoint(), astExprNodeMap, astPatternNodeMap, propertyEvalSpec, false);
            }
            return new ContextSpecKeyed(rawSpecs, optionalInit, optionalTermination);
        } else if (ctx.COALESCE() != null) {
            // hash
            List<EsperEPL2GrammarParser.CreateContextCoalesceItemContext> coalesces = ctx.createContextCoalesceItem();
            List<ContextSpecHashItem> rawSpecs = new ArrayList<ContextSpecHashItem>(coalesces.size());
            for (EsperEPL2GrammarParser.CreateContextCoalesceItemContext coalesce : coalesces) {
                ExprChainedSpec func = ASTLibFunctionHelper.getLibFunctionChainSpec(coalesce.libFunctionNoClass(), astExprNodeMap);
                FilterSpecRaw filterSpec = ASTFilterSpecHelper.walkFilterSpec(coalesce.eventFilterExpression(), propertyEvalSpec, astExprNodeMap);
                propertyEvalSpec = null;
                rawSpecs.add(new ContextSpecHashItem(func, filterSpec));
            }

            String granularity = ctx.g.getText();
            if (!granularity.toLowerCase(Locale.ENGLISH).equals("granularity")) {
                throw ASTWalkException.from("Expected 'granularity' keyword after list of coalesce items, found '" + granularity + "' instead");
            }
            Number num = (Number) ASTConstantHelper.parse(ctx.number());
            String preallocateStr = ctx.p != null ? ctx.p.getText() : null;
            if (preallocateStr != null && !preallocateStr.toLowerCase(Locale.ENGLISH).equals("preallocate")) {
                throw ASTWalkException.from("Expected 'preallocate' keyword after list of coalesce items, found '" + preallocateStr + "' instead");
            }
            if (!JavaClassHelper.isNumericNonFP(num.getClass()) || JavaClassHelper.getBoxedType(num.getClass()) == Long.class) {
                throw ASTWalkException.from("Granularity provided must be an int-type number, received " + num.getClass() + " instead");
            }

            return new ContextSpecHash(rawSpecs, num.intValue(), preallocateStr != null);
        }

        // categorized
        if (ctx.createContextGroupItem() != null) {
            List<EsperEPL2GrammarParser.CreateContextGroupItemContext> grps = ctx.createContextGroupItem();
            List<ContextSpecCategoryItem> items = new ArrayList<ContextSpecCategoryItem>();
            for (EsperEPL2GrammarParser.CreateContextGroupItemContext grp : grps) {
                ExprNode exprNode = ASTExprHelper.exprCollectSubNodes(grp, 0, astExprNodeMap).get(0);
                String name = grp.i.getText();
                items.add(new ContextSpecCategoryItem(exprNode, name));
            }
            FilterSpecRaw filterSpec = ASTFilterSpecHelper.walkFilterSpec(ctx.eventFilterExpression(), propertyEvalSpec, astExprNodeMap);
            return new ContextSpecCategory(items, filterSpec);
        }

        throw new IllegalStateException("Unrecognized context detail type");
    }

    private static List<ContextSpecConditionFilter> getContextPartitionInit(List<EsperEPL2GrammarParser.CreateContextFilterContext> ctxs, Map<Tree, ExprNode> astExprNodeMap) {
        List<ContextSpecConditionFilter> filters = new ArrayList<>(ctxs.size());
        for (EsperEPL2GrammarParser.CreateContextFilterContext ctx : ctxs) {
            filters.add(getContextDetailConditionFilter(ctx, null, astExprNodeMap));
        }
        return filters;
    }

    private static ContextSpecCondition getContextCondition(EsperEPL2GrammarParser.CreateContextRangePointContext ctx, Map<Tree, ExprNode> astExprNodeMap, Map<Tree, EvalForgeNode> astPatternNodeMap, PropertyEvalSpec propertyEvalSpec, boolean immediate) {
        if (ctx == null) {
            return ContextSpecConditionNever.INSTANCE;
        }
        if (ctx.crontabLimitParameterSetList() != null) {
            List<List<ExprNode>> crontabs = new ArrayList<>();
            for (EsperEPL2GrammarParser.CrontabLimitParameterSetContext crontabCtx : ctx.crontabLimitParameterSetList().crontabLimitParameterSet()) {
                List<ExprNode> crontab = ASTExprHelper.exprCollectSubNodes(crontabCtx, 0, astExprNodeMap);
                crontabs.add(crontab);
            }
            return new ContextSpecConditionCrontab(crontabs, immediate);
        } else if (ctx.patternInclusionExpression() != null) {
            EvalForgeNode evalNode = ASTExprHelper.patternGetRemoveTopNode(ctx.patternInclusionExpression(), astPatternNodeMap);
            boolean inclusive = false;
            if (ctx.i != null) {
                String ident = ctx.i.getText();
                if (ident != null && !ident.toLowerCase(Locale.ENGLISH).equals("inclusive")) {
                    throw ASTWalkException.from("Expected 'inclusive' keyword after '@', found '" + ident + "' instead");
                }
                inclusive = true;
            }
            return new ContextSpecConditionPattern(evalNode, inclusive, immediate);
        } else if (ctx.createContextFilter() != null) {
            if (immediate) {
                throw ASTWalkException.from("Invalid use of 'now' with initiated-by stream, this combination is not supported");
            }
            return getContextDetailConditionFilter(ctx.createContextFilter(), propertyEvalSpec, astExprNodeMap);
        } else if (ctx.AFTER() != null) {
            ExprTimePeriod timePeriod = (ExprTimePeriod) ASTExprHelper.exprCollectSubNodes(ctx.timePeriod(), 0, astExprNodeMap).get(0);
            return new ContextSpecConditionTimePeriod(timePeriod, immediate);
        } else {
            throw new IllegalStateException("Unrecognized child type");
        }
    }

    private static ContextSpecConditionFilter getContextDetailConditionFilter(EsperEPL2GrammarParser.CreateContextFilterContext ctx, PropertyEvalSpec propertyEvalSpec, Map<Tree, ExprNode> astExprNodeMap) {
        FilterSpecRaw filterSpecRaw = ASTFilterSpecHelper.walkFilterSpec(ctx.eventFilterExpression(), propertyEvalSpec, astExprNodeMap);
        String asName = ctx.i != null ? ctx.i.getText() : null;
        return new ContextSpecConditionFilter(filterSpecRaw, asName);
    }

    private static boolean checkNow(Token i) {
        if (i == null) {
            return false;
        }
        String ident = i.getText();
        if (!ident.toLowerCase(Locale.ENGLISH).equals("now")) {
            throw ASTWalkException.from("Expected 'now' keyword after '@', found '" + ident + "' instead");
        }
        return true;
    }
}
