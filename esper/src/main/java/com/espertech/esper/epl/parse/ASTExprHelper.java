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

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.epl.expression.ops.ExprMathNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodImpl;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.pattern.EvalFactoryNode;
import com.espertech.esper.rowregex.RowRegexExprNode;
import com.espertech.esper.type.MathArithTypeEnum;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ASTExprHelper {

    public static ExprNode resolvePropertyOrVariableIdentifier(String identifier, VariableService variableService, StatementSpecRaw spec) {
        VariableMetaData metaData = variableService.getVariableMetaData(identifier);
        if (metaData != null) {
            ExprVariableNodeImpl exprNode = new ExprVariableNodeImpl(metaData, null);
            spec.setHasVariables(true);
            addVariableReference(spec, metaData.getVariableName());
            String message = VariableServiceUtil.checkVariableContextName(spec.getOptionalContextName(), metaData);
            if (message != null) {
                throw ASTWalkException.from(message);
            }
            return exprNode;
        } else {
            return new ExprIdentNodeImpl(identifier);
        }
    }

    public static void addVariableReference(StatementSpecRaw statementSpec, String variableName) {
        if (statementSpec.getReferencedVariables() == null) {
            statementSpec.setReferencedVariables(new HashSet<String>());
        }
        statementSpec.getReferencedVariables().add(variableName);
    }

    public static ExprTimePeriod timePeriodGetExprAllParams(EsperEPL2GrammarParser.TimePeriodContext ctx, Map<Tree, ExprNode> astExprNodeMap, VariableService variableService, StatementSpecRaw spec, ConfigurationInformation config, TimeAbacus timeAbacus) {

        ExprNode[] nodes = new ExprNode[9];
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree unitRoot = ctx.getChild(i);

            ExprNode valueExpr;
            if (ASTUtil.isTerminatedOfType(unitRoot.getChild(0), EsperEPL2GrammarLexer.IDENT)) {
                String ident = unitRoot.getChild(0).getText();
                valueExpr = ASTExprHelper.resolvePropertyOrVariableIdentifier(ident, variableService, spec);
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

        ExprTimePeriod timeNode = new ExprTimePeriodImpl(config.getEngineDefaults().getExpression().getTimeZone(),
                nodes[0] != null, nodes[1] != null, nodes[2] != null, nodes[3] != null, nodes[4] != null, nodes[5] != null, nodes[6] != null, nodes[7] != null, nodes[8] != null, timeAbacus);

        for (ExprNode node : nodes) {
            if (node != null) timeNode.addChildNode(node);
        }
        return timeNode;
    }

    public static ExprTimePeriod timePeriodGetExprJustSeconds(EsperEPL2GrammarParser.ExpressionContext expression, Map<Tree, ExprNode> astExprNodeMap, ConfigurationInformation config, TimeAbacus timeAbacus) {
        ExprNode node = exprCollectSubNodes(expression, 0, astExprNodeMap).get(0);
        ExprTimePeriod timeNode = new ExprTimePeriodImpl(config.getEngineDefaults().getExpression().getTimeZone(),
                false, false, false, false, false, false, true, false, false, timeAbacus);
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

    public static void patternCollectAddSubnodesAddParentNode(EvalFactoryNode evalNode, Tree node, Map<Tree, EvalFactoryNode> astPatternNodeMap) {
        if (evalNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree childNode = node.getChild(i);
            EvalFactoryNode childEvalNode = patternGetRemoveTopNode(childNode, astPatternNodeMap);
            if (childEvalNode != null) {
                evalNode.addChildNode(childEvalNode);
            }
        }
        astPatternNodeMap.put(node, evalNode);
    }

    public static EvalFactoryNode patternGetRemoveTopNode(Tree node, Map<Tree, EvalFactoryNode> astPatternNodeMap) {
        EvalFactoryNode pattern = astPatternNodeMap.get(node);
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

    public static void regExCollectAddSubNodesAddParentNode(RowRegexExprNode exprNode, Tree node, Map<Tree, RowRegexExprNode> astRegExNodeMap) {
        regExCollectAddSubNodes(exprNode, node, astRegExNodeMap);
        astRegExNodeMap.put(node, exprNode);
    }

    public static void regExCollectAddSubNodes(final RowRegexExprNode regexNode, Tree node, Map<Tree, RowRegexExprNode> astRegExNodeMap) {
        if (regexNode == null) {
            throw ASTWalkException.from("Invalid null expression node for '" + ASTUtil.printNode(node) + "'");
        }
        RegExAction action = new RegExAction() {
            public void found(RowRegexExprNode exprNode, Map<Tree, RowRegexExprNode> astRegExNodeMap, Tree node) {
                astRegExNodeMap.remove(node);
                regexNode.addChildNode(exprNode);
            }
        };
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree childNode = node.getChild(i);
            regExApplyActionRecursive(childNode, astRegExNodeMap, action);
        }
    }

    public static void regExApplyActionRecursive(Tree node, Map<Tree, RowRegexExprNode> astRegExNodeMap, RegExAction action) {
        RowRegexExprNode expr = astRegExNodeMap.get(node);
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

    public static RowRegexExprNode regExGetRemoveTopNode(Tree node, Map<Tree, RowRegexExprNode> astRowRegexNodeMap) {
        RowRegexExprNode regex = astRowRegexNodeMap.get(node);
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

    public static ExprNode mathGetExpr(ParseTree ctx, Map<Tree, ExprNode> astExprNodeMap, ConfigurationInformation configurationInformation) {

        int count = 1;
        ExprNode base = ASTExprHelper.exprCollectSubNodes(ctx.getChild(0), 0, astExprNodeMap).get(0);

        while (true) {
            int token = ASTUtil.getAssertTerminatedTokenType(ctx.getChild(count));
            MathArithTypeEnum mathArithTypeEnum = tokenToMathEnum(token);

            ExprNode right = ASTExprHelper.exprCollectSubNodes(ctx.getChild(count + 1), 0, astExprNodeMap).get(0);

            ExprMathNode math = new ExprMathNode(mathArithTypeEnum,
                    configurationInformation.getEngineDefaults().getExpression().isIntegerDivision(),
                    configurationInformation.getEngineDefaults().getExpression().isDivisionByZeroReturnsNull());
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

    public static void addOptionalSimpleProperty(ExprNode exprNode, Token token, VariableService variableService, StatementSpecRaw spec) {
        if (token == null) {
            return;
        }
        ExprNode node = ASTExprHelper.resolvePropertyOrVariableIdentifier(token.getText(), variableService, spec);
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
        public void found(RowRegexExprNode exprNode, Map<Tree, RowRegexExprNode> astRegExNodeMap, Tree node);
    }
}
