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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import com.espertech.esper.compiler.internal.parse.CaseChangingCharStreamFactory;
import com.espertech.esper.compiler.internal.parse.ParseHelper;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class EPLModuleUtil {
    private final static Logger log = LoggerFactory.getLogger(EPLModuleUtil.class);

    /**
     * Newline character.
     */
    public static final String NEWLINE = System.getProperty("line.separator");

    public static Module readInternal(InputStream stream, String resourceName) throws IOException, ParseException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringWriter buffer = new StringWriter();
        String strLine;
        while ((strLine = br.readLine()) != null) {
            buffer.append(strLine);
            buffer.append(NEWLINE);
        }
        stream.close();

        return parseInternal(buffer.toString(), resourceName);
    }

    public static Module parseInternal(String buffer, String resourceName) throws IOException, ParseException {

        List<EPLModuleParseItem> semicolonSegments = EPLModuleUtil.parse(buffer.toString());
        List<ParseNode> nodes = new ArrayList<ParseNode>();
        for (EPLModuleParseItem segment : semicolonSegments) {
            nodes.add(EPLModuleUtil.getModule(segment, resourceName));
        }

        String moduleName = null;
        int count = 0;
        for (ParseNode node : nodes) {
            if (node instanceof ParseNodeComment) {
                continue;
            }
            if (node instanceof ParseNodeModule) {
                if (moduleName != null) {
                    throw new ParseException("Duplicate use of the 'module' keyword for resource '" + resourceName + "'");
                }
                if (count > 0) {
                    throw new ParseException("The 'module' keyword must be the first declaration in the module file for resource '" + resourceName + "'");
                }
                moduleName = ((ParseNodeModule) node).getModuleName();
            }
            count++;
        }

        Set<String> uses = new LinkedHashSet<String>();
        Set<String> imports = new LinkedHashSet<String>();
        count = 0;
        for (ParseNode node : nodes) {
            if ((node instanceof ParseNodeComment) || (node instanceof ParseNodeModule)) {
                continue;
            }
            String message = "The 'uses' and 'import' keywords must be the first declaration in the module file or follow the 'module' declaration";
            if (node instanceof ParseNodeUses) {
                if (count > 0) {
                    throw new ParseException(message);
                }
                uses.add(((ParseNodeUses) node).getUses());
                continue;
            }
            if (node instanceof ParseNodeImport) {
                if (count > 0) {
                    throw new ParseException(message);
                }
                imports.add(((ParseNodeImport) node).getImported());
                continue;
            }
            count++;
        }

        List<ModuleItem> items = new ArrayList<ModuleItem>();
        for (ParseNode node : nodes) {
            if ((node instanceof ParseNodeComment) || (node instanceof ParseNodeExpression)) {
                boolean isComments = node instanceof ParseNodeComment;
                items.add(new ModuleItem(node.getItem().getExpression(), isComments, node.getItem().getLineNum(), node.getItem().getStartChar(), node.getItem().getEndChar(), node.getItem().getLineNumEnd(), isComments ? -1 : node.getItem().getLineNumContent(), isComments ? -1 : node.getItem().getLineNumContentEnd()));
            }
        }

        return new Module(moduleName, resourceName, uses, imports, items, buffer);
    }

    public static ParseNode getModule(EPLModuleParseItem item, String resourceName) throws ParseException, IOException {
        CharStream input = CaseChangingCharStreamFactory.make(item.getExpression());
        EsperEPL2GrammarLexer lex = ParseHelper.newLexer(input);
        CommonTokenStream tokenStream = new CommonTokenStream(lex);
        tokenStream.fill();

        List tokens = tokenStream.getTokens();
        int beginIndex = 0;
        boolean isMeta = false;
        boolean isModule = false;
        boolean isUses = false;
        boolean isExpression = false;

        while (beginIndex < tokens.size()) {
            Token t = (Token) tokens.get(beginIndex);
            if (t.getType() == EsperEPL2GrammarParser.EOF) {
                break;
            }
            if ((t.getType() == EsperEPL2GrammarParser.WS) ||
                (t.getType() == EsperEPL2GrammarParser.SL_COMMENT) ||
                (t.getType() == EsperEPL2GrammarParser.ML_COMMENT)) {
                beginIndex++;
                continue;
            }
            String tokenText = t.getText().trim().toLowerCase(Locale.ENGLISH);
            if (tokenText.equals("module")) {
                isModule = true;
                isMeta = true;
            } else if (tokenText.equals("uses")) {
                isUses = true;
                isMeta = true;
            } else if (tokenText.equals("import")) {
                isMeta = true;
            } else {
                isExpression = true;
                break;
            }
            beginIndex++;
            beginIndex++;   // skip space
            break;
        }

        if (isExpression) {
            return new ParseNodeExpression(item);
        }
        if (!isMeta) {
            return new ParseNodeComment(item);
        }

        // check meta tag (module, uses, import)
        StringWriter buffer = new StringWriter();
        for (int i = beginIndex; i < tokens.size(); i++) {
            Token t = (Token) tokens.get(i);
            if (t.getType() == EsperEPL2GrammarParser.EOF) {
                break;
            }
            if ((t.getType() != EsperEPL2GrammarParser.IDENT) &&
                (t.getType() != EsperEPL2GrammarParser.DOT) &&
                (t.getType() != EsperEPL2GrammarParser.STAR) &&
                (!t.getText().matches("[a-zA-Z]*"))) {
                throw getMessage(isModule, isUses, resourceName, t.getType());
            }
            buffer.append(t.getText().trim());
        }

        String result = buffer.toString().trim();
        if (result.length() == 0) {
            throw getMessage(isModule, isUses, resourceName, -1);
        }

        if (isModule) {
            return new ParseNodeModule(item, result);
        } else if (isUses) {
            return new ParseNodeUses(item, result);
        }
        return new ParseNodeImport(item, result);
    }

    private static ParseException getMessage(boolean module, boolean uses, String resourceName, int type) {
        String message = "Keyword '";
        if (module) {
            message += "module";
        } else if (uses) {
            message += "uses";
        } else {
            message += "import";
        }
        message += "' must be followed by a name or package name (set of names separated by dots) for resource '" + resourceName + "'";

        if (type != -1) {
            String tokenName = EsperEPL2GrammarParser.getLexerTokenParaphrases().get(type);
            if (tokenName == null) {
                tokenName = EsperEPL2GrammarParser.getParserTokenParaphrases().get(type);
            }
            if (tokenName != null) {
                message += ", unexpected reserved keyword " + tokenName + " was encountered as part of the name";
            }
        }
        return new ParseException(message);
    }

    public static List<EPLModuleParseItem> parse(String module) throws ParseException {

        CharStream input = CaseChangingCharStreamFactory.make(module);
        EsperEPL2GrammarLexer lex = ParseHelper.newLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        try {
            tokens.fill();
        } catch (RuntimeException ex) {
            String message = "Unexpected exception recognizing module text";
            if (ex instanceof LexerNoViableAltException) {
                if (ParseHelper.hasControlCharacters(module)) {
                    message = "Unrecognized control characters found in text, failed to parse text";
                } else {
                    message += ", recognition failed for " + ex.toString();
                }
            } else if (ex instanceof RecognitionException) {
                RecognitionException recog = (RecognitionException) ex;
                message += ", recognition failed for " + recog.toString();
            } else if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            }
            message += " [" + module + "]";
            throw new ParseException(message);
        }

        List<EPLModuleParseItem> statements = new ArrayList<EPLModuleParseItem>();
        StringWriter current = new StringWriter();
        Integer startLineNum = null;
        Integer startLineNumContent = null;
        int charPosStart = 0;
        int charPos = 0;
        List<Token> tokenList = tokens.getTokens();
        Set<Integer> skippedSemicolonIndexes = getSkippedSemicolons(tokenList);
        int index = -1;
        int lastLineNum = -1;
        int lastLineNumContent = -1;
        for (Object token : tokenList) {
            index++;
            Token t = (Token) token;
            lastLineNum = t.getLine();
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                lastLineNumContent = t.getLine();
            }
            boolean semi = t.getType() == EsperEPL2GrammarLexer.SEMI && !skippedSemicolonIndexes.contains(index);
            if (semi) {
                if (current.toString().trim().length() > 0) {
                    statements.add(new EPLModuleParseItem(current.toString().trim(), startLineNum == null ? 0 : startLineNum, charPosStart, charPos, t.getLine(), startLineNumContent == null ? 0 : startLineNumContent, lastLineNumContent));
                    startLineNum = null;
                    startLineNumContent = null;
                }
                current = new StringWriter();
            } else {
                if (startLineNum == null && t.getType() != EsperEPL2GrammarParser.WS) {
                    startLineNum = t.getLine();
                    charPosStart = charPos;
                }
                if (startLineNumContent == null && t.getType() != EsperEPL2GrammarParser.WS && t.getChannel() == Token.DEFAULT_CHANNEL) {
                    startLineNumContent = t.getLine();
                }
                if (t.getType() != EsperEPL2GrammarLexer.EOF) {
                    current.append(t.getText());
                    charPos += t.getText().length();
                }
            }
        }

        if (current.toString().trim().length() > 0) {
            statements.add(new EPLModuleParseItem(current.toString().trim(), startLineNum == null ? 0 : startLineNum, 0, 0, lastLineNum, startLineNumContent == null ? 0 : startLineNumContent, lastLineNumContent));
        }
        return statements;
    }

    public static Module readFile(File file) throws IOException, ParseException {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return EPLModuleUtil.readInternal(inputStream, file.getAbsolutePath());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.debug("Error closing input stream", e);
                }
            }
        }
    }

    public static Module readResource(String resource, ClassLoader classLoader) throws IOException, ParseException {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;

        InputStream stream = null;
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = EPLModuleUtil.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = EPLModuleUtil.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new IOException("Failed to find resource '" + resource + "' in classpath");
        }

        try {
            return EPLModuleUtil.readInternal(stream, resource);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                log.debug("Error closing input stream", e);
            }
        }
    }

    /**
     * Find expression declarations and skip semicolon content between square brackets for scripts
     */
    private static Set<Integer> getSkippedSemicolons(List<Token> tokens) {
        Set<Integer> result = null;

        int index = 0;
        while (index < tokens.size()) {
            Token t = tokens.get(index);
            if (t.getType() == EsperEPL2GrammarParser.EXPRESSIONDECL) {
                if (result == null) {
                    result = new HashSet<>();
                }
                index = getSkippedSemicolonsBetweenSquareBrackets(index, tokens, result);
            }
            if (t.getType() == EsperEPL2GrammarParser.CLASSDECL) {
                if (result == null) {
                    result = new HashSet<>();
                }
                index = getSkippedSemicolonsBetweenTripleQuotes(index, tokens, result);
            }
            index++;
        }

        return result == null ? Collections.<Integer>emptySet() : result;
    }

    /**
     * Find content between square brackets
     */
    private static int getSkippedSemicolonsBetweenSquareBrackets(int index, List<Token> tokens, Set<Integer> result) {
        // Handle EPL expression "{text}" and script expression "[text]"
        int indexFirstCurly = indexFirstToken(index, tokens, EsperEPL2GrammarParser.LCURLY);
        int indexFirstSquare = indexFirstToken(index, tokens, EsperEPL2GrammarParser.LBRACK);
        if (indexFirstSquare == -1) {
            return index;
        }
        if (indexFirstCurly != -1 && indexFirstCurly < indexFirstSquare) {
            return index;
        }
        int indexCloseSquare = findEndSquareBrackets(indexFirstSquare, tokens);
        if (indexCloseSquare == -1) {
            return index;
        }

        if (indexFirstSquare == indexCloseSquare - 1) {
            getSkippedSemicolonsBetweenSquareBrackets(indexCloseSquare, tokens, result);
        } else {
            getSkippedSemicolonsBetweenIndexes(indexFirstSquare, indexCloseSquare, tokens, result);
        }
        return indexCloseSquare;
    }


    /**
     * Find content between triple quotes
     */
    private static int getSkippedSemicolonsBetweenTripleQuotes(int index, List<Token> tokens, Set<Integer> result) {
        // Handle class """{text}"""
        int indexFirstTriple = indexFirstToken(index, tokens, EsperEPL2GrammarParser.TRIPLEQUOTE);
        if (indexFirstTriple == -1) {
            return index;
        }
        int indexCloseTriple = indexFirstToken(indexFirstTriple + 1, tokens, EsperEPL2GrammarParser.TRIPLEQUOTE);
        if (indexCloseTriple == -1) {
            return index;
        }
        getSkippedSemicolonsBetweenIndexes(indexFirstTriple, indexCloseTriple, tokens, result);
        return indexCloseTriple;
    }

    private static int findEndSquareBrackets(int startIndex, List<Token> tokens) {
        int index = startIndex + 1;
        int squareBracketDepth = 0;
        while (index < tokens.size()) {
            Token t = tokens.get(index);
            if (t.getType() == EsperEPL2GrammarParser.RBRACK) {
                if (squareBracketDepth == 0) {
                    return index;
                }
                squareBracketDepth--;
            }
            if (t.getType() == EsperEPL2GrammarParser.LBRACK) {
                squareBracketDepth++;
            }
            index++;
        }
        return -1;
    }

    private static int indexFirstToken(int startIndex, List<Token> tokens, int tokenType) {
        int index = startIndex;
        while (index < tokens.size()) {
            Token t = tokens.get(index);
            if (t.getType() == tokenType) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static void getSkippedSemicolonsBetweenIndexes(int indexOpen, int indexClose, List<Token> tokens, Set<Integer> result) {
        int current = indexOpen;
        while (current < indexClose) {
            Token t = tokens.get(current);
            if (t.getType() == EsperEPL2GrammarParser.SEMI) {
                result.add(current);
            }
            current++;
        }
    }
}
