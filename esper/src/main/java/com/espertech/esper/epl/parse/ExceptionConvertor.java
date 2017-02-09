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

import com.espertech.esper.client.EPStatementSyntaxException;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

/**
 * Converts recognition exceptions.
 */
public class ExceptionConvertor {
    private static final Logger log = LoggerFactory.getLogger(ExceptionConvertor.class);

    protected final static String END_OF_INPUT_TEXT = "end-of-input";

    /**
     * Converts from a syntax error to a nice statement exception.
     *
     * @param e              is the syntax error
     * @param expression     is the expression text
     * @param parser         the parser that parsed the expression
     * @param addPleaseCheck indicates to add "please check" paraphrases
     * @return syntax exception
     */
    public static EPStatementSyntaxException convertStatement(RecognitionException e, String expression, boolean addPleaseCheck, EsperEPL2GrammarParser parser) {
        UniformPair<String> pair = convert(e, expression, addPleaseCheck, parser);
        return new EPStatementSyntaxException(pair.getFirst(), pair.getSecond());
    }

    /**
     * Converts from a syntax error to a nice property exception.
     *
     * @param e              is the syntax error
     * @param expression     is the expression text
     * @param parser         the parser that parsed the expression
     * @param addPleaseCheck indicates to add "please check" paraphrases
     * @return syntax exception
     */
    public static PropertyAccessException convertProperty(RecognitionException e, String expression, boolean addPleaseCheck, EsperEPL2GrammarParser parser) {
        UniformPair<String> pair = convert(e, expression, addPleaseCheck, parser);
        return new PropertyAccessException(pair.getFirst(), pair.getSecond());
    }

    /**
     * Converts from a syntax error to a nice exception.
     *
     * @param e              is the syntax error
     * @param expression     is the expression text
     * @param parser         the parser that parsed the expression
     * @param addPleaseCheck indicates to add "please check" paraphrases
     * @return syntax exception
     */
    public static UniformPair<String> convert(RecognitionException e, String expression, boolean addPleaseCheck, EsperEPL2GrammarParser parser) {
        if (expression.trim().length() == 0) {
            String message = "Unexpected " + END_OF_INPUT_TEXT;
            return new UniformPair<String>(message, expression);
        }

        Token t;
        Token tBeforeBefore = null;
        Token tBefore = null;
        Token tAfter = null;

        int tIndex = e.getOffendingToken() != null ? e.getOffendingToken().getTokenIndex() : Integer.MAX_VALUE;
        if (tIndex < parser.getTokenStream().size()) {
            t = parser.getTokenStream().get(tIndex);
            if ((tIndex + 1) < parser.getTokenStream().size()) {
                tAfter = parser.getTokenStream().get(tIndex + 1);
            }
            if (tIndex - 1 >= 0) {
                tBefore = parser.getTokenStream().get(tIndex - 1);
            }
            if (tIndex - 2 >= 0) {
                tBeforeBefore = parser.getTokenStream().get(tIndex - 2);
            }
        } else {
            if (parser.getTokenStream().size() >= 1) {
                tBeforeBefore = parser.getTokenStream().get(parser.getTokenStream().size() - 1);
            }
            if (parser.getTokenStream().size() >= 2) {
                tBefore = parser.getTokenStream().get(parser.getTokenStream().size() - 2);
            }
            t = parser.getTokenStream().get(parser.getTokenStream().size() - 1);
        }

        Token tEnd = null;
        if (parser.getTokenStream().size() > 0) {
            tEnd = parser.getTokenStream().get(parser.getTokenStream().size() - 1);
        }

        String positionInfo = getPositionInfo(t);
        String token = t.getType() == EsperEPL2GrammarParser.EOF ? "end-of-input" : "'" + t.getText() + "'";

        Stack stack = parser.getParaphrases();
        String check = "";
        boolean isSelect = stack.size() == 1 && stack.get(0).equals("select clause");
        if ((stack.size() > 0) && addPleaseCheck) {
            String delimiter = "";
            StringBuilder checkList = new StringBuilder();
            checkList.append(", please check the ");
            while (stack.size() != 0) {
                checkList.append(delimiter);
                checkList.append(stack.pop());
                delimiter = " within the ";
            }
            check = checkList.toString();
        }

        // check if token is a reserved keyword
        Set<String> keywords = parser.getKeywords();
        boolean reservedKeyword = false;
        if (keywords.contains(token.toLowerCase(Locale.ENGLISH))) {
            token += " (a reserved keyword)";
            reservedKeyword = true;
        } else if (tAfter != null && keywords.contains("'" + tAfter.getText().toLowerCase(Locale.ENGLISH) + "'")) {
            token += " ('" + tAfter.getText() + "' is a reserved keyword)";
            reservedKeyword = true;
        } else {
            if ((tBefore != null) &&
                    (tAfter != null) &&
                    (keywords.contains("'" + tBefore.getText().toLowerCase(Locale.ENGLISH) + "'")) &&
                    (keywords.contains("'" + tAfter.getText().toLowerCase(Locale.ENGLISH) + "'"))) {
                token += " ('" + tBefore.getText() + "' and '" + tAfter.getText() + "' are a reserved keyword)";
                reservedKeyword = true;
            } else if ((tBefore != null) &&
                    (keywords.contains("'" + tBefore.getText().toLowerCase(Locale.ENGLISH) + "'"))) {
                token += " ('" + tBefore.getText() + "' is a reserved keyword)";
                reservedKeyword = true;
            } else if (tEnd != null && keywords.contains("'" + tEnd.getText().toLowerCase(Locale.ENGLISH) + "'")) {
                token += " ('" + tEnd.getText() + "' is a reserved keyword)";
                reservedKeyword = true;
            }
        }

        // special handling for the select-clause "as" keyword, which is required
        if (isSelect && !reservedKeyword) {
            check += getSelectClauseAsText(tBeforeBefore, t);
        }

        String message = "Incorrect syntax near " + token + positionInfo + check;
        if (e instanceof NoViableAltException || e instanceof LexerNoViableAltException || checkForInputMismatchWithNoExpected(e)) {
            Token nvaeToken = e.getOffendingToken();
            int nvaeTokenType = nvaeToken != null ? nvaeToken.getType() : EsperEPL2GrammarLexer.EOF;

            if (nvaeTokenType == EsperEPL2GrammarLexer.EOF) {
                if (token.equals(END_OF_INPUT_TEXT)) {
                    message = "Unexpected " + END_OF_INPUT_TEXT + positionInfo + check;
                } else {
                    if (ParseHelper.hasControlCharacters(expression)) {
                        message = "Unrecognized control characters found in text" + positionInfo;
                    } else {
                        message = "Unexpected " + END_OF_INPUT_TEXT + " near " + token + positionInfo + check;
                    }
                }
            } else {
                if (parser.getParserTokenParaphrases().get(nvaeTokenType) != null) {
                    message = "Incorrect syntax near " + token + positionInfo + check;
                } else {
                    // find next keyword in the next 3 tokens
                    int currentIndex = tIndex + 1;
                    while ((currentIndex > 0) &&
                            (currentIndex < parser.getTokenStream().size() - 1) &&
                            (currentIndex < tIndex + 3)) {
                        Token next = parser.getTokenStream().get(currentIndex);
                        currentIndex++;

                        String quotedToken = "'" + next.getText() + "'";
                        if (parser.getKeywords().contains(quotedToken)) {
                            check += " near reserved keyword '" + next.getText() + "'";
                            break;
                        }
                    }
                    message = "Incorrect syntax near " + token + positionInfo + check;
                }
            }
        } else if (e instanceof InputMismatchException) {
            InputMismatchException mismatched = (InputMismatchException) e;

            String expected;
            if (mismatched.getExpectedTokens().size() > 1) {
                StringWriter writer = new StringWriter();
                writer.append("any of the following tokens {");
                String delimiter = "";
                for (int i = 0; i < mismatched.getExpectedTokens().size(); i++) {
                    writer.append(delimiter);
                    if (i > 5) {
                        writer.append("...");
                        writer.append(Integer.toString(mismatched.getExpectedTokens().size() - 5));
                        writer.append(" more");
                        break;
                    }
                    delimiter = ", ";
                    writer.append(getTokenText(parser, mismatched.getExpectedTokens().get(i)));
                }
                writer.append("}");
                expected = writer.toString();
            } else {
                expected = getTokenText(parser, mismatched.getExpectedTokens().get(0));
            }

            int offendingTokenType = mismatched.getOffendingToken().getType();
            String unexpected = getTokenText(parser, offendingTokenType);

            String expecting = " expecting " + expected.trim() + " but found " + unexpected.trim();
            message = "Incorrect syntax near " + token + expecting + positionInfo + check;
        }

        return new UniformPair<String>(message, expression);
    }

    private static boolean checkForInputMismatchWithNoExpected(RecognitionException e) {
        if (!(e instanceof InputMismatchException)) {
            return false;
        }
        if (e.getExpectedTokens().size() > 1) {
            return false;
        }
        return e.getExpectedTokens().size() == 1 && e.getExpectedTokens().get(0) == -1;
    }

    private static String getTokenText(EsperEPL2GrammarParser parser, int tokenIndex) {
        String expected = END_OF_INPUT_TEXT;
        if ((tokenIndex >= 0) && (tokenIndex < parser.getTokenNames().length)) {
            expected = parser.getTokenNames()[tokenIndex];
        }
        if (parser.getLexerTokenParaphrases().get(tokenIndex) != null) {
            expected = parser.getLexerTokenParaphrases().get(tokenIndex);
        }
        if (parser.getParserTokenParaphrases().get(tokenIndex) != null) {
            expected = parser.getParserTokenParaphrases().get(tokenIndex);
        }
        return expected;
    }

    /**
     * Returns the position information string for a parser exception.
     *
     * @param t the token to return the information for
     * @return is a string with line and column information
     */
    private static String getPositionInfo(Token t) {
        return t.getLine() > 0 && t.getCharPositionInLine() > 0
                ? " at line " + t.getLine() + " column " + t.getCharPositionInLine()
                : "";
    }

    private static String getSelectClauseAsText(Token tBeforeBefore, Token t) {
        if (tBeforeBefore != null &&
                tBeforeBefore.getType() == EsperEPL2GrammarParser.IDENT &&
                t != null &&
                t.getType() == EsperEPL2GrammarParser.IDENT) {
            return " (did you forget 'as'?)";
        }
        return "";
    }
}
