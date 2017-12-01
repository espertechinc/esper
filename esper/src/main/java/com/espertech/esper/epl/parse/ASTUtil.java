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

import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.util.StringValue;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static com.espertech.esper.util.StringValue.unescapeBacktick;

/**
 * Utility class for AST node handling.
 */
public class ASTUtil {
    private final static Logger log = LoggerFactory.getLogger(ASTUtil.class);

    private final static String PROPERTY_ENABLED_AST_DUMP = "ENABLE_AST_DUMP";

    public static List<String> getIdentList(EsperEPL2GrammarParser.ColumnListContext ctx) {
        if (ctx == null || ctx.isEmpty()) {
            return Collections.emptyList();
        }
        List<TerminalNode> idents = ctx.IDENT();
        List<String> parameters = new ArrayList<String>(idents.size());
        for (TerminalNode ident : idents) {
            parameters.add(ident.getText());
        }
        return parameters;
    }

    public static boolean isTerminatedOfType(Tree child, int tokenType) {
        if (!(child instanceof TerminalNode)) {
            return false;
        }
        TerminalNode termNode = (TerminalNode) child;
        return termNode.getSymbol().getType() == tokenType;
    }

    public static int getRuleIndexIfProvided(ParseTree tree) {
        if (!(tree instanceof RuleNode)) {
            return -1;
        }
        RuleNode ruleNode = (RuleNode) tree;
        return ruleNode.getRuleContext().getRuleIndex();
    }

    public static int getAssertTerminatedTokenType(ParseTree child) {
        if (!(child instanceof TerminalNode)) {
            throw ASTWalkException.from("Unexpected exception walking AST, expected terminal node", child.getText());
        }
        TerminalNode term = (TerminalNode) child;
        return term.getSymbol().getType();
    }

    public static String printNode(Tree node) {
        StringWriter buf = new StringWriter();
        PrintWriter writer = new PrintWriter(buf);
        ASTUtil.dumpAST(writer, node, 0);
        return buf.toString();
    }

    public static boolean isRecursiveParentRule(ParserRuleContext ctx, Set<Integer> rulesIds) {
        ParserRuleContext parent = ctx.getParent();
        if (parent == null) {
            return false;
        }
        return rulesIds.contains(parent.getRuleIndex()) || isRecursiveParentRule(parent, rulesIds);
    }

    /**
     * Dump the AST node to system.out.
     *
     * @param ast to dump
     */
    public static void dumpAST(Tree ast) {
        if (System.getProperty(PROPERTY_ENABLED_AST_DUMP) != null) {
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);

            renderNode(new char[0], ast, printer);
            dumpAST(printer, ast, 2);

            log.info(".dumpAST ANTLR Tree dump follows...\n" + writer.toString());
        }
    }

    public static void dumpAST(PrintWriter printer, Tree ast, int ident) {
        char[] identChars = new char[ident];
        Arrays.fill(identChars, ' ');

        if (ast == null) {
            renderNode(identChars, null, printer);
            return;
        }
        for (int i = 0; i < ast.getChildCount(); i++) {
            Tree node = ast.getChild(i);
            if (node == null) {
                throw new NullPointerException("Null AST node");
            }
            renderNode(identChars, node, printer);
            dumpAST(printer, node, ident + 2);
        }
    }

    /**
     * Print the token stream to the logger.
     *
     * @param tokens to print
     */
    public static void printTokens(CommonTokenStream tokens) {
        if (log.isDebugEnabled()) {
            List tokenList = tokens.getTokens();

            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            for (int i = 0; i < tokens.size(); i++) {
                Token t = (Token) tokenList.get(i);
                String text = t.getText();
                if (text.trim().length() == 0) {
                    printer.print("'" + text + "'");
                } else {
                    printer.print(text);
                }
                printer.print('[');
                printer.print(t.getType());
                printer.print(']');
                printer.print(" ");
            }
            printer.println();
            log.debug("Tokens: " + writer.toString());
        }
    }

    private static void renderNode(char[] ident, Tree node, PrintWriter printer) {
        printer.print(ident);
        if (node == null) {
            printer.print("NULL NODE");
        } else {
            if (node instanceof ParserRuleContext) {
                ParserRuleContext ctx = (ParserRuleContext) node;
                int ruleIndex = ctx.getRuleIndex();
                String ruleName = EsperEPL2GrammarParser.ruleNames[ruleIndex];
                printer.print(ruleName);
            } else {
                TerminalNode terminal = (TerminalNode) node;
                printer.print(terminal.getSymbol().getText());
                printer.print(" [");
                printer.print(terminal.getSymbol().getType());
                printer.print("]");
            }

            if (node instanceof ParseTree) {
                ParseTree parseTree = (ParseTree) node;
                if (parseTree.getText() == null) {
                    printer.print(" (null value in text)");
                } else if (parseTree.getText().contains("\\")) {
                    int count = 0;
                    for (int i = 0; i < parseTree.getText().length(); i++) {
                        if (parseTree.getText().charAt(i) == '\\') {
                            count++;
                        }
                    }
                    printer.print(" (" + count + " backlashes)");
                }
            }
        }
        printer.println();
    }

    /**
     * Escape all unescape dot characters in the text (identifier only) passed in.
     *
     * @param identifierToEscape text to escape
     * @return text where dots are escaped
     */
    protected static String escapeDot(String identifierToEscape) {
        int indexof = identifierToEscape.indexOf(".");
        if (indexof == -1) {
            return identifierToEscape;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < identifierToEscape.length(); i++) {
            char c = identifierToEscape.charAt(i);
            if (c != '.') {
                builder.append(c);
                continue;
            }

            if (i > 0) {
                if (identifierToEscape.charAt(i - 1) == '\\') {
                    builder.append('.');
                    continue;
                }
            }

            builder.append('\\');
            builder.append('.');
        }

        return builder.toString();
    }

    /**
     * Un-Escape all escaped dot characters in the text (identifier only) passed in.
     *
     * @param identifierToUnescape text to un-escape
     * @return string
     */
    public static String unescapeDot(String identifierToUnescape) {
        int indexof = identifierToUnescape.indexOf(".");
        if (indexof == -1) {
            return identifierToUnescape;
        }
        indexof = identifierToUnescape.indexOf("\\");
        if (indexof == -1) {
            return identifierToUnescape;
        }

        StringBuilder builder = new StringBuilder();
        int index = -1;
        int max = identifierToUnescape.length() - 1;
        do {
            index++;
            char c = identifierToUnescape.charAt(index);
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (index < identifierToUnescape.length() - 1) {
                if (identifierToUnescape.charAt(index + 1) == '.') {
                    builder.append('.');
                    index++;
                }
            }
        }
        while (index < max);

        return builder.toString();
    }

    public static String getPropertyName(EsperEPL2GrammarParser.EventPropertyContext ctx, int startNode) {
        StringBuilder buf = new StringBuilder();
        for (int i = startNode; i < ctx.getChildCount(); i++) {
            ParseTree tree = ctx.getChild(i);
            buf.append(tree.getText());
        }
        return buf.toString();
    }

    public static String unescapeClassIdent(EsperEPL2GrammarParser.ClassIdentifierContext classIdentCtx) {
        return unescapeEscapableStr(classIdentCtx.escapableStr(), ".");
    }

    public static String unescapeSlashIdentifier(EsperEPL2GrammarParser.SlashIdentifierContext ctx) {
        String name = unescapeEscapableStr(ctx.escapableStr(), "/");
        if (ctx.d != null) {
            name = "/" + name;
        }
        return name;
    }

    private static String unescapeEscapableStr(List<EsperEPL2GrammarParser.EscapableStrContext> ctxs, String delimiterConst) {
        if (ctxs.size() == 1) {
            return unescapeBacktick(unescapeDot(ctxs.get(0).getText()));
        }

        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (EsperEPL2GrammarParser.EscapableStrContext ctx : ctxs) {
            writer.append(delimiter);
            writer.append(unescapeBacktick(unescapeDot(ctx.getText())));
            delimiter = delimiterConst;
        }

        return writer.toString();
    }

    public static String getStreamNameUnescapedOptional(EsperEPL2GrammarParser.IdentOrTickedContext ctx) {
        return ctx != null ? StringValue.unescapeBacktick(ctx.getText()) : null;
    }
}
