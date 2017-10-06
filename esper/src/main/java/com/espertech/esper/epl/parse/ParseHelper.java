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

import com.espertech.esper.client.EPException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Helper class for parsing an expression and walking a parse tree.
 */
public class ParseHelper {
    /**
     * Newline.
     */
    public final static String NEWLINE = System.getProperty("line.separator");

    /**
     * Walk parse tree starting at the rule the walkRuleSelector supplies.
     *
     * @param ast                     - ast to walk
     * @param listener                - walker instance
     * @param expression              - the expression we are walking in string form
     * @param eplStatementForErrorMsg - statement text for error messages
     */
    public static void walk(Tree ast, EPLTreeWalkerListener listener, String expression, String eplStatementForErrorMsg) {
        // Walk tree
        try {
            if (log.isDebugEnabled()) {
                log.debug(".walk Walking AST using walker " + listener.getClass().getName());
            }
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(listener, (ParseTree) ast);
            listener.end();
        } catch (RuntimeException e) {
            log.info("Error walking statement [" + expression + "]", e);
            throw e;
        }
    }

    /**
     * Parse expression using the rule the ParseRuleSelector instance supplies.
     *
     * @param expression           - text to parse
     * @param parseRuleSelector    - parse rule to select
     * @param addPleaseCheck       - true to include depth paraphrase
     * @param eplStatementErrorMsg - text for error
     * @param rewriteScript        - whether to rewrite script expressions
     * @return AST - syntax tree
     * @throws EPException when the AST could not be parsed
     */
    public static ParseResult parse(String expression, String eplStatementErrorMsg, boolean addPleaseCheck, ParseRuleSelector parseRuleSelector, boolean rewriteScript) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug(".parse Parsing expr=" + expression);
        }

        CharStream input = new CaseInsensitiveInputStream(expression);
        EsperEPL2GrammarLexer lex = newLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lex);
        EsperEPL2GrammarParser parser = ParseHelper.newParser(tokens);

        Tree tree;
        try {
            tree = parseRuleSelector.invokeParseRule(parser);
        } catch (RecognitionException ex) {
            tokens.fill();
            if (rewriteScript && isContainsScriptExpression(tokens)) {
                return handleScriptRewrite(tokens, eplStatementErrorMsg, addPleaseCheck, parseRuleSelector);
            }
            log.debug("Error parsing statement [" + expression + "]", ex);
            throw ExceptionConvertor.convertStatement(ex, eplStatementErrorMsg, addPleaseCheck, parser);
        } catch (RuntimeException e) {
            try {
                tokens.fill();
            } catch (RuntimeException ex) {
                log.debug("Token-fill produced exception: " + e.getMessage(), e);
            }
            if (log.isDebugEnabled()) {
                log.debug("Error parsing statement [" + eplStatementErrorMsg + "]", e);
            }
            if (e.getCause() instanceof RecognitionException) {
                if (rewriteScript && isContainsScriptExpression(tokens)) {
                    return handleScriptRewrite(tokens, eplStatementErrorMsg, addPleaseCheck, parseRuleSelector);
                }
                throw ExceptionConvertor.convertStatement((RecognitionException) e.getCause(), eplStatementErrorMsg, addPleaseCheck, parser);
            } else {
                throw e;
            }
        }

        // if we are re-writing scripts and contain a script, then rewrite
        if (rewriteScript && isContainsScriptExpression(tokens)) {
            return handleScriptRewrite(tokens, eplStatementErrorMsg, addPleaseCheck, parseRuleSelector);
        }

        if (log.isDebugEnabled()) {
            log.debug(".parse Dumping AST...");
            ASTUtil.dumpAST(tree);
        }

        String expressionWithoutAnnotation = expression;
        if (tree instanceof EsperEPL2GrammarParser.StartEPLExpressionRuleContext) {
            EsperEPL2GrammarParser.StartEPLExpressionRuleContext epl = (EsperEPL2GrammarParser.StartEPLExpressionRuleContext) tree;
            expressionWithoutAnnotation = getNoAnnotation(expression, epl.annotationEnum(), tokens);
        } else if (tree instanceof EsperEPL2GrammarParser.StartPatternExpressionRuleContext) {
            EsperEPL2GrammarParser.StartPatternExpressionRuleContext pattern = (EsperEPL2GrammarParser.StartPatternExpressionRuleContext) tree;
            expressionWithoutAnnotation = getNoAnnotation(expression, pattern.annotationEnum(), tokens);
        }

        return new ParseResult(tree, expressionWithoutAnnotation, tokens, Collections.<String>emptyList());
    }

    private static ParseResult handleScriptRewrite(CommonTokenStream tokens, String eplStatementErrorMsg, boolean addPleaseCheck, ParseRuleSelector parseRuleSelector) {
        ScriptResult rewriteExpression = rewriteTokensScript(tokens);
        ParseResult result = parse(rewriteExpression.getRewrittenEPL(), eplStatementErrorMsg, addPleaseCheck, parseRuleSelector, false);
        return new ParseResult(result.getTree(), result.getExpressionWithoutAnnotations(), result.getTokenStream(), rewriteExpression.getScripts());
    }

    private static String getNoAnnotation(String expression, List<EsperEPL2GrammarParser.AnnotationEnumContext> annos, CommonTokenStream tokens) {
        if (annos == null || annos.isEmpty()) {
            return expression;
        }
        Token lastAnnotationToken = annos.get(annos.size() - 1).getStop();

        if (lastAnnotationToken == null) {
            return null;
        }

        try {
            int line = lastAnnotationToken.getLine();
            int charpos = lastAnnotationToken.getCharPositionInLine();
            int fromChar = charpos + lastAnnotationToken.getText().length();
            if (line == 1) {
                return expression.substring(fromChar).trim();
            }

            String[] lines = expression.split("\r\n|\r|\n");
            StringBuilder buf = new StringBuilder();
            buf.append(lines[line - 1].substring(fromChar));
            for (int i = line; i < lines.length; i++) {
                buf.append(lines[i]);
                if (i < lines.length - 1) {
                    buf.append(NEWLINE);
                }
            }
            return buf.toString().trim();
        } catch (RuntimeException ex) {
            log.error("Error determining non-annotated expression sting: " + ex.getMessage(), ex);
        }
        return null;
    }

    private static ScriptResult rewriteTokensScript(CommonTokenStream tokens) {
        List<String> scripts = new ArrayList<String>();

        List<UniformPair<Integer>> scriptTokenIndexRanges = new ArrayList<UniformPair<Integer>>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == EsperEPL2GrammarParser.EXPRESSIONDECL) {
                Token tokenBefore = getTokenBefore(i, tokens);
                boolean isCreateExpressionClause = tokenBefore != null && tokenBefore.getType() == EsperEPL2GrammarParser.CREATE;
                Pair<String, Integer> nameAndNameStart = findScriptName(i + 1, tokens);

                int startIndex = findStartTokenScript(nameAndNameStart.getSecond(), tokens, EsperEPL2GrammarParser.LBRACK);
                if (startIndex != -1) {
                    int endIndex = findEndTokenScript(startIndex + 1, tokens, EsperEPL2GrammarParser.RBRACK, EsperEPL2GrammarParser.getAfterScriptTokens(), !isCreateExpressionClause);
                    if (endIndex != -1) {

                        StringWriter writer = new StringWriter();
                        for (int j = startIndex + 1; j < endIndex; j++) {
                            writer.append(tokens.get(j).getText());
                        }
                        scripts.add(writer.toString());
                        scriptTokenIndexRanges.add(new UniformPair<Integer>(startIndex, endIndex));
                    }
                }
            }
        }

        String rewrittenEPL = rewriteScripts(scriptTokenIndexRanges, tokens);
        return new ScriptResult(rewrittenEPL, scripts);
    }

    private static Token getTokenBefore(int i, CommonTokenStream tokens) {
        int position = i - 1;
        while (position >= 0) {
            Token t = tokens.get(position);
            if (t.getChannel() != 99 && t.getType() != EsperEPL2GrammarLexer.WS) {
                return t;
            }
            position--;
        }
        return null;
    }

    private static Pair<String, Integer> findScriptName(int start, CommonTokenStream tokens) {
        String lastIdent = null;
        int lastIdentIndex = 0;
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == EsperEPL2GrammarParser.IDENT) {
                lastIdent = tokens.get(i).getText();
                lastIdentIndex = i;
            }
            if (tokens.get(i).getType() == EsperEPL2GrammarParser.LPAREN) {
                break;
            }
            // find beginning of script, ignore brackets
            if (tokens.get(i).getType() == EsperEPL2GrammarParser.LBRACK && tokens.get(i + 1).getType() != EsperEPL2GrammarParser.RBRACK) {
                break;
            }
        }
        if (lastIdent == null) {
            throw new IllegalStateException("Failed to parse expression name");
        }
        return new Pair<String, Integer>(lastIdent, lastIdentIndex);
    }


    private static String rewriteScripts(List<UniformPair<Integer>> ranges, CommonTokenStream tokens) {
        if (ranges.isEmpty()) {
            return tokens.getText();
        }
        StringWriter writer = new StringWriter();
        int rangeIndex = 0;
        UniformPair<Integer> current = ranges.get(rangeIndex);
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() == EsperEPL2GrammarLexer.EOF) {
                break;
            }
            if (i < current.getFirst()) {
                writer.append(t.getText());
            } else if (i == current.getFirst()) {
                writer.append(t.getText());
                writer.append("'");
            } else if (i == current.getSecond()) {
                writer.append("'");
                writer.append(t.getText());
                rangeIndex++;
                if (ranges.size() > rangeIndex) {
                    current = ranges.get(rangeIndex);
                } else {
                    current = new UniformPair<Integer>(-1, -1);
                }
            } else if (t.getType() == EsperEPL2GrammarParser.SL_COMMENT || t.getType() == EsperEPL2GrammarParser.ML_COMMENT) {
                writeCommentEscapeSingleQuote(writer, t);
            } else {
                if (t.getType() == EsperEPL2GrammarParser.QUOTED_STRING_LITERAL && i > current.getFirst() && i < current.getSecond()) {
                    writer.append("\\'");
                    writer.append(t.getText().substring(1, t.getText().length() - 1));
                    writer.append("\\'");
                } else {
                    writer.append(t.getText());
                }
            }
        }
        return writer.toString();
    }

    private static int findEndTokenScript(int startIndex, CommonTokenStream tokens, int tokenTypeSearch, Set<Integer> afterScriptTokens, boolean requireAfterScriptToken) {

        // The next non-comment token must be among the afterScriptTokens, i.e. SELECT/INSERT/ON/DELETE/UPDATE
        // Find next non-comment token.
        if (requireAfterScriptToken) {
            int found = -1;
            for (int i = startIndex; i < tokens.size(); i++) {
                if (tokens.get(i).getType() == tokenTypeSearch) {
                    for (int j = i + 1; j < tokens.size(); j++) {
                        Token next = tokens.get(j);
                        if (next.getChannel() == 0) {
                            if (afterScriptTokens.contains(next.getType())) {
                                found = i;
                            }
                            break;
                        }
                    }
                }
                if (found != -1) {
                    break;
                }
            }
            return found;
        }

        // Find the last token
        int indexLast = -1;
        for (int i = startIndex; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == tokenTypeSearch) {
                indexLast = i;
            }
        }
        return indexLast;
    }

    private static boolean isContainsScriptExpression(CommonTokenStream tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == EsperEPL2GrammarParser.EXPRESSIONDECL) {
                int startTokenLcurly = findStartTokenScript(i + 1, tokens, EsperEPL2GrammarParser.LCURLY);
                int startTokenLbrack = findStartTokenScript(i + 1, tokens, EsperEPL2GrammarParser.LBRACK);
                // Handle:
                // expression ABC { some[other] }
                // expression boolean js:doit(...) [ {} ]
                if (startTokenLbrack != -1 && (startTokenLcurly == -1 || startTokenLcurly > startTokenLbrack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int findStartTokenScript(int startIndex, CommonTokenStream tokens, int tokenTypeSearch) {
        int found = -1;
        for (int i = startIndex; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == tokenTypeSearch) {
                return i;
            }
        }
        return found;
    }

    public static EsperEPL2GrammarLexer newLexer(CharStream input) {
        EsperEPL2GrammarLexer lex = new EsperEPL2GrammarLexer(input);
        lex.removeErrorListeners();
        lex.addErrorListener(Antlr4ErrorListener.INSTANCE);
        return lex;
    }

    public static EsperEPL2GrammarParser newParser(CommonTokenStream tokens) {
        EsperEPL2GrammarParser g = new EsperEPL2GrammarParser(tokens);
        g.removeErrorListeners();
        g.addErrorListener(Antlr4ErrorListener.INSTANCE);
        g.setErrorHandler(new Antlr4ErrorStrategy());
        return g;
    }

    public static boolean hasControlCharacters(String text) {
        String textWithoutControlCharacters = text.replaceAll("\\p{Cc}", "");
        return !textWithoutControlCharacters.equals(text);
    }

    private static void writeCommentEscapeSingleQuote(StringWriter writer, Token t) {
        String text = t.getText();
        if (!text.contains("'")) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                writer.append('\\');
                writer.append(c);
            } else {
                writer.append(c);
            }
        }
    }

    private static class ScriptResult {
        private final String rewrittenEPL;
        private final List<String> scripts;

        private ScriptResult(String rewrittenEPL, List<String> scripts) {
            this.rewrittenEPL = rewrittenEPL;
            this.scripts = scripts;
        }

        public String getRewrittenEPL() {
            return rewrittenEPL;
        }

        public List<String> getScripts() {
            return scripts;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ParseHelper.class);
}
