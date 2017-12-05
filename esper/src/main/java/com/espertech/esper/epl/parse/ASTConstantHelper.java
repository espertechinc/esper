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

import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.util.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;

/**
 * Parses constant strings and returns the constant Object.
 */
public class ASTConstantHelper {

    /**
     * Parse the AST constant node and return Object value.
     *
     * @param node - parse node for which to parse the string value
     * @return value matching AST node type
     */
    public static Object parse(ParseTree node) {
        if (node instanceof TerminalNode) {
            TerminalNode terminal = (TerminalNode) node;
            switch (terminal.getSymbol().getType()) {
                case EsperEPL2GrammarParser.BOOLEAN_TRUE:
                    return BoolValue.parseString(terminal.getText());
                case EsperEPL2GrammarParser.BOOLEAN_FALSE:
                    return BoolValue.parseString(terminal.getText());
                case EsperEPL2GrammarParser.VALUE_NULL:
                    return null;
                default:
                    throw ASTWalkException.from("Encountered unexpected constant type " + terminal.getSymbol().getType(), terminal.getSymbol());
            }
        } else {
            RuleNode ruleNode = (RuleNode) node;
            int ruleIndex = ruleNode.getRuleContext().getRuleIndex();
            if (ruleIndex == EsperEPL2GrammarParser.RULE_number) {
                return parseNumber(ruleNode, 1);
            } else if (ruleIndex == EsperEPL2GrammarParser.RULE_numberconstant) {
                RuleNode number = findChildRuleByType(ruleNode, EsperEPL2GrammarParser.RULE_number);
                if (ruleNode.getChildCount() > 1) {
                    if (ASTUtil.isTerminatedOfType(ruleNode.getChild(0), EsperEPL2GrammarLexer.MINUS)) {
                        return parseNumber(number, -1);
                    }
                    return parseNumber(number, 1);
                } else {
                    return parseNumber(number, 1);
                }
            } else if (ruleIndex == EsperEPL2GrammarParser.RULE_stringconstant) {
                return StringValue.parseString(node.getText());
            } else if (ruleIndex == EsperEPL2GrammarParser.RULE_constant) {
                return parse(ruleNode.getChild(0));
            }
            throw ASTWalkException.from("Encountered unrecognized constant", node.getText());
        }
    }

    private static Object parseNumber(RuleNode number, int factor) {
        int tokenType = getSingleChildTokenType(number);
        if (tokenType == EsperEPL2GrammarLexer.IntegerLiteral) {
            return parseIntLongByte(number.getText(), factor);
        } else if (tokenType == EsperEPL2GrammarLexer.FloatingPointLiteral) {
            String numberText = number.getText();
            if (numberText.endsWith("f") || numberText.endsWith("F")) {
                return Float.parseFloat(number.getText()) * factor;
            } else {
                return Double.parseDouble(number.getText()) * factor;
            }
        }
        throw ASTWalkException.from("Encountered unrecognized constant", number.getText());
    }

    private static Object parseIntLongByte(String arg, int factor) {
        // try to parse as an int first, else try to parse as a long
        try {
            return Integer.parseInt(arg) * factor;
        } catch (NumberFormatException e1) {
            try {
                return LongValue.parseString(arg) * factor;
            } catch (Exception e2) {
                try {
                    return Byte.decode(arg);
                } catch (Exception e3) {
                    throw e1;
                }
            }
        }
    }

    private static RuleNode findChildRuleByType(Tree node, int ruleNum) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Tree child = node.getChild(i);
            if (isRuleOfType(child, ruleNum)) {
                return (RuleNode) child;
            }
        }
        return null;
    }

    private static boolean isRuleOfType(Tree child, int ruleNum) {
        if (!(child instanceof RuleNode)) {
            return false;
        }
        RuleNode ruleNode = (RuleNode) child;
        return ruleNode.getRuleContext().getRuleIndex() == ruleNum;
    }

    private static int getSingleChildTokenType(RuleNode node) {
        return ((TerminalNode) node.getChild(0)).getSymbol().getType();
    }
}
