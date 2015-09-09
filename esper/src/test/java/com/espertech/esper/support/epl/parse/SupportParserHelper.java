/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.epl.parse;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.parse.*;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceImpl;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.support.bean.SupportBean_N;
import com.espertech.esper.support.core.SupportEngineImportServiceFactory;
import com.espertech.esper.support.event.SupportEventAdapterService;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringReader;

public class SupportParserHelper
{
    public static EPLTreeWalkerListener parseAndWalkEPL(String expression) throws Exception
    {
        return parseAndWalkEPL(expression, SupportEngineImportServiceFactory.make(), new VariableServiceImpl(0, null, SupportEventAdapterService.getService(), null));
    }

    public static EPLTreeWalkerListener parseAndWalkEPL(String expression, EngineImportService engineImportService, VariableService variableService) throws Exception
    {
        log.debug(".parseAndWalk Trying text=" + expression);
        Pair<Tree, CommonTokenStream> ast = SupportParserHelper.parseEPL(expression);
        log.debug(".parseAndWalk success, tree walking...");
        SupportParserHelper.displayAST(ast.getFirst());

        EventAdapterService eventAdapterService = SupportEventAdapterService.getService();
        eventAdapterService.addBeanType("SupportBean_N", SupportBean_N.class, true, true, true);

        EPLTreeWalkerListener listener = SupportEPLTreeWalkerFactory.makeWalker(ast.getSecond(), engineImportService, variableService);
        ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        walker.walk(listener, (ParseTree) ast.getFirst()); // initiate walk of tree with listener
        return listener;
    }

    public static EPLTreeWalkerListener parseAndWalkPattern(String expression) throws Exception
    {
        log.debug(".parseAndWalk Trying text=" + expression);
        Pair<Tree, CommonTokenStream> ast = SupportParserHelper.parsePattern(expression);
        log.debug(".parseAndWalk success, tree walking...");
        SupportParserHelper.displayAST(ast.getFirst());

        EPLTreeWalkerListener listener = SupportEPLTreeWalkerFactory.makeWalker(ast.getSecond());
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, (ParseTree) ast.getFirst());
        return listener;
    }

    public static void displayAST(Tree ast) throws Exception
    {
        log.debug(".displayAST...");
        if (log.isDebugEnabled()) {
            ASTUtil.dumpAST(ast);
        }
    }

    public static Pair<Tree, CommonTokenStream> parsePattern(String text) throws Exception
    {
        ParseRuleSelector startRuleSelector = new ParseRuleSelector()
        {
            public ParserRuleContext invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException
            {
                return parser.startPatternExpressionRule();
            }
        };
        return parse(startRuleSelector, text);
    }

    public static Pair<Tree, CommonTokenStream> parseEPL(String text) throws Exception
    {
        ParseRuleSelector startRuleSelector = new ParseRuleSelector()
        {
            public ParserRuleContext invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException
            {
                return parser.startEPLExpressionRule();
            }
        };
        return parse(startRuleSelector, text);
    }

    public static Pair<Tree, CommonTokenStream> parseEventProperty(String text) throws Exception
    {
        ParseRuleSelector startRuleSelector = new ParseRuleSelector()
        {
            public ParserRuleContext invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException
            {
                return parser.startEventPropertyRule();
            }
        };
        return parse(startRuleSelector, text);
    }

    public static Pair<Tree, CommonTokenStream> parseJson(String text) throws Exception
    {
        ParseRuleSelector startRuleSelector = new ParseRuleSelector()
        {
            public ParserRuleContext invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException
            {
                return parser.startJsonValueRule();
            }
        };
        return parse(startRuleSelector, text);
    }

    public static Pair<Tree, CommonTokenStream> parse(ParseRuleSelector parseRuleSelector, String text) throws Exception
    {
        CharStream input;
        try
        {
            input = new NoCaseSensitiveStream(new StringReader(text));
        }
        catch (IOException ex)
        {
            throw new RuntimeException("IOException parsing text '" + text + '\'', ex);
        }

        EsperEPL2GrammarLexer lex = ParseHelper.newLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lex);
        EsperEPL2GrammarParser g = ParseHelper.newParser(tokens);

        Tree ctx = parseRuleSelector.invokeParseRule(g);
        return new Pair<Tree, CommonTokenStream>(ctx, tokens);
    }

    private static Log log = LogFactory.getLog(SupportParserHelper.class);
}
