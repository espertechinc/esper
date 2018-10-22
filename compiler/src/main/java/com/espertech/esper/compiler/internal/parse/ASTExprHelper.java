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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.common.internal.epl.expression.ops.ExprMathNode;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodImpl;
import com.espertech.esper.common.internal.epl.expression.variable.ExprVariableNodeImpl;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.rowrecog.expr.RowRecogExprNode;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.type.MathArithTypeEnum;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ASTExprHelper {

    public static ExprNode resolvePropertyOrVariableIdentifier(String identifier, VariableCompileTimeResolver variableCompileTimeResolver, StatementSpecRaw spec) {
        VariableMetaData metaData = variableCompileTimeResolver.resolve(identifier);
        if (metaData != null) {
            ExprVariableNodeImpl exprNode = new ExprVariableNodeImpl(metaData, null);
            spec.getReferencedVariables().add(metaData.getVariableName());
            String message = VariableUtil.checkVariableContextName(spec.getOptionalContextName(), metaData);
            if (message != null) {
                throw ASTWalkException.from(message);
            }
            return exprNode;
        }
        return new ExprIdentNodeImpl(identifier);
    }

    public static ExprTimePeriod timePeriodGetExprAllParams(EsperEPL2GrammarParser.TimePeriodContext ctx, Map<Tree, ExprNode> astExprNodeMap, VariableCompileTimeResolver variableCompileTimeResolver, StatementSpecRaw spec, Configuration config, TimeAbacus timeAbacus) {

        ExprNode[] nodes = new ExprNode[9];
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree unitRoot = ctx.getChild(i);

            ExprNode valueExpr;
            if (ASTUtil.isTerminatedOfType(unitRoot.getChild(0), EsperEPL2GrammarLexer.IDENT)) {
                String ident = unitRoot.getChild(0).getText();
                valueExpr = ASTExprHelper.resolvePropertyOrVariableIdentifier(ident, variableCompileTimeResolver, spec);
            } else {
                final AtomicReference<ExprNode> ref = new AtomicReference<ExprNode>();
                ExprAction action = new ExprAction() {
                    public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node) {
                        astExprNodeMap.remove(node);
                        ref.set(exprNode);
                    }
                };
                ASTExprHelper.recursiveFindRemoveChildExprNode(unitRoot.getChild(0), astExprNodeMap, action);
                valueExpr = ref.get();
            }

            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_microsecondPart) {
                nodes[8] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_millisecondPart) {
                nodes[7] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_secondPart) {
                nodes[6] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_minutePart) {
                nodes[5] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_hourPart) {
                nodes[4] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_dayPart) {
                nodes[3] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_weekPart) {
                nodes[2] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_monthPart) {
                nodes[1] = valueExpr;
            }
            if (ASTUtil.getRuleIndexIfProvided(unitRoot) == EsperEPL2GrammarParser.RULE_yearPart) {
                nodes[0] = valueExpr;
            }
        }

        ExprTimePeriod timeNode = new ExprTimePeriodImpl(nodes[0] != null, nodes[1] != null, nodes[2] != null, nodes[3] != null, nodes[4] != null, nodes[5] != null, nodes[6] != null, nodes[7] != null, nodes[8] != null, timeAbacus);

        for (ExprNode node : nodes) {
            if (node != null) timeNode.addChildNode(node);
        }
        return timeNode;
    }

    public static ExprTimePeriod timePeriodGetExprJustSeconds(EsperEPL2GrammarParser.ExpressionContext expression, Map<Tree, ExprNode> astExprNodeMap, TimeAbacus timeAbacus) {
        ExprNode node = exprCollectSubNodes(expression, 0, astExprNodeMap).get(0);
        ExprTimePeriod timeNode = new ExprTimePeriodImpl(false, false, false, false, false, false, true, false, false, timeAbacus);
        timeNode.addChildNode(node);
        return timeNode;
    }

    protected static List<OnTriggerSetAssignment> getOnTriggerSetAssignments(EsperEPL2GrammarParser.OnSetAssignmentListContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        if (ctx == null || ctx.onSetAssignment().isEmpty()) {
            return Collections.emptyList();
        }
        List<EsperEPL2GrammarParser.OnSetAssignmentContext> ctxs = ctx.onSetAssignment();
        List<OnTriggerSetAssignment> assignments = new ArrayList<OnTriggerSetAssignment>(ctx.onSetAssignment().size());
        for (EsperEPL2GrammarParser.OnSetAssignmentContext assign : ctxs) {
            ExprNode childEvalNode;
            if (assign.eventProperty() != null) {
                ExprNode prop = ASTExprHelper.exprCollectSubNodes(assign.eventProperty(), 0, astExprNodeMap).get(0);
                ExprNode value = ASTExprHelper.exprCollectSubNodes(assign.expression(), 0, astExprNodeMap).get(0);
                ExprEqualsNode equals = new ExprEqualsNodeImpl(false, false);
                equals.addChildNode(prop);
                equals.addChildNode(value);
                childEvalNode = equals;
            } else {
                childEvalNode = ASTExprHelper.exprCollectSubNodes(assign, 0, astExprNodeMap).get(0);
            }
            assignments.add(new OnTriggerSetAssignment(childEvalNode));
        }
        return assignments;
    }

    public static void patternCollectAddSubnodesAddParentNode(EvalForgeNode evalNode, Tree node, Map<Tree, EvalForgeNode> astPatternNodeMap) {
        if (evalNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree childNode = node.getChild(i);
            EvalForgeNode childEvalNode = patternGetRemoveTopNode(childNode, astPatternNodeMap);
            if (childEvalNode != null) {
                evalNode.addChildNode(childEvalNode);
            }
        }
        astPatternNodeMap.put(node, evalNode);
    }

    public static EvalForgeNode patternGetRemoveTopNode(Tree node, Map<Tree, EvalForgeNode> astPatternNodeMap) {
        EvalForgeNode pattern = astPatternNodeMap.get(node);
        if (pattern != null) {
            astPatternNodeMap.remove(node);
            return pattern;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            pattern = patternGetRemoveTopNode(node.getChild(i), astPatternNodeMap);
            if (pattern != null) {
                return pattern;
            }
        }
        return null;
    }

    public static void regExCollectAddSubNodesAddParentNode(RowRecogExprNode exprNode, Tree node, Map<Tree, RowRecogExprNode> astRegExNodeMap) {
        regExCollectAddSubNodes(exprNode, node, astRegExNodeMap);
        astRegExNodeMap.put(node, exprNode);
    }

    public static void regExCollectAddSubNodes(final RowRecogExprNode regexNode, Tree node, Map<Tree, RowRecogExprNode> astRegExNodeMap) {
        if (regexNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        RegExAction action = new RegExAction() {
            public void found(RowRecogExprNode exprNode, Map<Tree, RowRecogExprNode> astRegExNodeMap, Tree node) {
                astRegExNodeMap.remove(node);
                regexNode.addChildNode(exprNode);
            }
        };
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree childNode = node.getChild(i);
            regExApplyActionRecursive(childNode, astRegExNodeMap, action);
        }
    }

    public static void regExApplyActionRecursive(Tree node, Map<Tree, RowRecogExprNode> astRegExNodeMap, RegExAction action) {
        RowRecogExprNode expr = astRegExNodeMap.get(node);
        if (expr != null) {
            action.found(expr, astRegExNodeMap, node);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            regExApplyActionRecursive(node.getChild(i), astRegExNodeMap, action);
        }
    }

    public static void exprCollectAddSubNodesAddParentNode(ExprNode exprNode, Tree node, Map<Tree, ExprNode> astExprNodeMap) {
        exprCollectAddSubNodes(exprNode, node, astExprNodeMap);
        astExprNodeMap.put(node, exprNode);
    }

    public static void exprCollectAddSubNodes(final ExprNode parentNode, Tree node, Map<Tree, ExprNode> astExprNodeMap) {
        if (parentNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        if (node == null) {
            return;
        }
        ExprAction action = new ExprAction() {
            public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node) {
                astExprNodeMap.remove(node);
                parentNode.addChildNode(exprNode);
            }
        };
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree childNode = node.getChild(i);
            recursiveFindRemoveChildExprNode(childNode, astExprNodeMap, action);
        }
    }

    public static void exprCollectAddSingle(final ExprNode parentNode, Tree node, Map<Tree, ExprNode> astExprNodeMap) {
        if (parentNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        if (node == null) {
            return;
        }
        ExprAction action = new ExprAction() {
            public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node) {
                astExprNodeMap.remove(node);
                parentNode.addChildNode(exprNode);
            }
        };
        recursiveFindRemoveChildExprNode(node, astExprNodeMap, action);
    }

    public static void exprCollectAddSubNodesExpressionCtx(final ExprNode parentNode, List<EsperEPL2GrammarParser.ExpressionContext> expressionContexts, Map<Tree, ExprNode> astExprNodeMap) {
        ExprAction action = new ExprAction() {
            public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node) {
                astExprNodeMap.remove(node);
                parentNode.addChildNode(exprNode);
            }
        };
        for (EsperEPL2GrammarParser.ExpressionContext ctx : expressionContexts) {
            recursiveFindRemoveChildExprNode(ctx, astExprNodeMap, action);
        }
    }

    public static List<ExprNode> exprCollectSubNodes(Tree parentNode, int startIndex, Map<Tree, ExprNode> astExprNodeMap) {
        ExprNode selfNode = astExprNodeMap.remove(parentNode);
        if (selfNode != null) {
            return Collections.singletonList(selfNode);
        }
        final List<ExprNode> exprNodes = new ArrayList<ExprNode>();
        ExprAction action = new ExprAction() {
            public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node) {
                astExprNodeMap.remove(node);
                exprNodes.add(exprNode);
            }
        };
        for (int i = startIndex; i < parentNode.getChildCount(); i++) {
            Tree currentNode = parentNode.getChild(i);
            recursiveFindRemoveChildExprNode(currentNode, astExprNodeMap, action);
        }
        return exprNodes;
    }

    private static void recursiveFindRemoveChildExprNode(Tree node, Map<Tree, ExprNode> astExprNodeMap, ExprAction action) {
        ExprNode expr = astExprNodeMap.get(node);
        if (expr != null) {
            action.found(expr, astExprNodeMap, node);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            recursiveFindRemoveChildExprNode(node.getChild(i), astExprNodeMap, action);
        }
    }

    public static RowRecogExprNode regExGetRemoveTopNode(Tree node, Map<Tree, RowRecogExprNode> astRowRegexNodeMap) {
        RowRecogExprNode regex = astRowRegexNodeMap.get(node);
        if (regex != null) {
            astRowRegexNodeMap.remove(node);
            return regex;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            regex = regExGetRemoveTopNode(node.getChild(i), astRowRegexNodeMap);
            if (regex != null) {
                return regex;
            }
        }
        return null;
    }

    public static ExprNode mathGetExpr(ParseTree ctx, Map<Tree, ExprNode> astExprNodeMap, Configuration configurationInformation) {

        int count = 1;
        ExprNode base = ASTExprHelper.exprCollectSubNodes(ctx.getChild(0), 0, astExprNodeMap).get(0);

        while (true) {
            int token = ASTUtil.getAssertTerminatedTokenType(ctx.getChild(count));
            MathArithTypeEnum mathArithTypeEnum = tokenToMathEnum(token);

            ExprNode right = ASTExprHelper.exprCollectSubNodes(ctx.getChild(count + 1), 0, astExprNodeMap).get(0);

            ExprMathNode math = new ExprMathNode(mathArithTypeEnum,
                    configurationInformation.getCompiler().getExpression().isIntegerDivision(),
                    configurationInformation.getCompiler().getExpression().isDivisionByZeroReturnsNull());
            math.addChildNode(base);
            math.addChildNode(right);
            base = math;

            count += 2;
            if (count >= ctx.getChildCount()) {
                break;
            }
        }
        return base;
    }

    private static MathArithTypeEnum tokenToMathEnum(int token) {
        switch (token) {
            case EsperEPL2GrammarLexer.DIV:
                return MathArithTypeEnum.DIVIDE;
            case EsperEPL2GrammarLexer.STAR:
                return MathArithTypeEnum.MULTIPLY;
            case EsperEPL2GrammarLexer.PLUS:
                return MathArithTypeEnum.ADD;
            case EsperEPL2GrammarLexer.MINUS:
                return MathArithTypeEnum.SUBTRACT;
            case EsperEPL2GrammarLexer.MOD:
                return MathArithTypeEnum.MODULO;
            default:
                throw ASTWalkException.from("Encountered unrecognized math token type " + token);
        }
    }

    public static void addOptionalNumber(ExprNode exprNode, EsperEPL2GrammarParser.NumberContext number) {
        if (number == null) {
            return;
        }
        ExprConstantNode constantNode = new ExprConstantNodeImpl(ASTConstantHelper.parse(number));
        exprNode.addChildNode(constantNode);
    }

    public static void addOptionalSimpleProperty(ExprNode exprNode, Token token, VariableCompileTimeResolver variableCompileTimeResolver, StatementSpecRaw spec) {
        if (token == null) {
            return;
        }
        ExprNode node = ASTExprHelper.resolvePropertyOrVariableIdentifier(token.getText(), variableCompileTimeResolver, spec);
        exprNode.addChildNode(node);
    }

    public static ExprNode[] exprCollectSubNodesPerNode(List<EsperEPL2GrammarParser.ExpressionContext> expression, Map<Tree, ExprNode> astExprNodeMap) {
        ExprNode[] nodes = new ExprNode[expression.size()];
        for (int i = 0; i < expression.size(); i++) {
            nodes[i] = exprCollectSubNodes(expression.get(i), 0, astExprNodeMap).get(0);
        }
        return nodes;
    }

    private static interface ExprAction {
        public void found(ExprNode exprNode, Map<Tree, ExprNode> astExprNodeMap, Tree node);
    }

    private static interface RegExAction {
        public void found(RowRecogExprNode exprNode, Map<Tree, RowRecogExprNode> astRegExNodeMap, Tree node);
    }
}
