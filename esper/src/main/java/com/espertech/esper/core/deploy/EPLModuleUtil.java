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
package com.espertech.esper.core.deploy;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.deploy.ParseException;
import com.espertech.esper.core.service.StatementEventTypeRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.parse.CaseInsensitiveInputStream;
import com.espertech.esper.epl.parse.ParseHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.filter.FilterService;
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
                items.add(new ModuleItem(node.getItem().getExpression(), isComments, node.getItem().getLineNum(), node.getItem().getStartChar(), node.getItem().getEndChar()));
            }
        }

        return new Module(moduleName, resourceName, uses, imports, items, buffer);
    }

    public static List<EventType> undeployTypes(Set<String> referencedTypes, StatementEventTypeRef statementEventTypeRef, EventAdapterService eventAdapterService, FilterService filterService) {
        List<EventType> undeployedTypes = new ArrayList<EventType>();
        for (String typeName : referencedTypes) {

            boolean typeInUse = statementEventTypeRef.isInUse(typeName);
            if (typeInUse) {
                if (log.isDebugEnabled()) {
                    log.debug("Event type '" + typeName + "' is in use, not removing type");
                }
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("Event type '" + typeName + "' is no longer in use, removing type");
            }
            EventType type = eventAdapterService.getExistsTypeByName(typeName);
            if (type != null) {
                EventTypeSPI spi = (EventTypeSPI) type;
                if (!spi.getMetadata().isApplicationPreConfigured()) {
                    eventAdapterService.removeType(typeName);
                    undeployedTypes.add(spi);
                    filterService.removeType(spi);
                }
            }
        }
        return undeployedTypes;
    }

    public static ParseNode getModule(EPLModuleParseItem item, String resourceName) throws ParseException, IOException {
        CharStream input = new CaseInsensitiveInputStream(item.getExpression());
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

        CharStream input = new CaseInsensitiveInputStream(module);
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
            log.error(message, ex);
            throw new ParseException(message);
        }

        List<EPLModuleParseItem> statements = new ArrayList<EPLModuleParseItem>();
        StringWriter current = new StringWriter();
        Integer lineNum = null;
        int charPosStart = 0;
        int charPos = 0;
        List<Token> tokenList = tokens.getTokens();
        Set<Integer> skippedSemicolonIndexes = getSkippedSemicolons(tokenList);
        int index = -1;
        // Call getTokens first before invoking tokens.size! ANTLR problem
        for (Object token : tokenList) {
            index++;
            Token t = (Token) token;
            boolean semi = t.getType() == EsperEPL2GrammarLexer.SEMI && !skippedSemicolonIndexes.contains(index);
            if (semi) {
                if (current.toString().trim().length() > 0) {
                    statements.add(new EPLModuleParseItem(current.toString().trim(), lineNum == null ? 0 : lineNum, charPosStart, charPos));
                    lineNum = null;
                }
                current = new StringWriter();
            } else {
                if ((lineNum == null) && (t.getType() != EsperEPL2GrammarParser.WS)) {
                    lineNum = t.getLine();
                    charPosStart = charPos;
                }
                if (t.getType() != EsperEPL2GrammarLexer.EOF) {
                    current.append(t.getText());
                    charPos += t.getText().length();
                }
            }
        }

        if (current.toString().trim().length() > 0) {
            statements.add(new EPLModuleParseItem(current.toString().trim(), lineNum == null ? 0 : lineNum, 0, 0));
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

    public static Module readResource(String resource, EngineImportService engineImportService) throws IOException, ParseException {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;

        InputStream stream = null;
        ClassLoader classLoader = engineImportService.getClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = EPDeploymentAdminImpl.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = EPDeploymentAdminImpl.class.getClassLoader().getResourceAsStream(stripped);
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

        int index = -1;
        for (Object token : tokens) {
            index++;
            Token t = (Token) token;
            if (t.getType() == EsperEPL2GrammarParser.EXPRESSIONDECL) {
                if (result == null) {
                    result = new HashSet<Integer>();
                }
                getSkippedSemicolonsBetweenSquareBrackets(index, tokens, result);
            }
        }

        return result == null ? Collections.<Integer>emptySet() : result;
    }

    /**
     * Find content between square brackets
     */
    private static void getSkippedSemicolonsBetweenSquareBrackets(int index, List<Token> tokens, Set<Integer> result) {
        // Handle EPL expression "{text}" and script expression "[text]"
        int indexFirstCurly = indexFirstToken(index, tokens, EsperEPL2GrammarParser.LCURLY);
        int indexFirstSquare = indexFirstToken(index, tokens, EsperEPL2GrammarParser.LBRACK);
        if (indexFirstSquare == -1) {
            return;
        }
        if (indexFirstCurly != -1 && indexFirstCurly < indexFirstSquare) {
            return;
        }
        int indexCloseSquare = findEndSquareBrackets(indexFirstSquare, tokens);
        if (indexCloseSquare == -1) {
            return;
        }

        if (indexFirstSquare == indexCloseSquare - 1) {
            getSkippedSemicolonsBetweenSquareBrackets(indexCloseSquare, tokens, result);
        } else {
            int current = indexFirstSquare;
            while (current < indexCloseSquare) {
                Token t = tokens.get(current);
                if (t.getType() == EsperEPL2GrammarParser.SEMI) {
                    result.add(current);
                }
                current++;
            }
        }
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
}
