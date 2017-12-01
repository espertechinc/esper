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
package com.espertech.esper.event.property;

import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.epl.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.parse.CaseInsensitiveInputStream;
import com.espertech.esper.epl.parse.ExceptionConvertor;
import com.espertech.esper.epl.parse.ParseHelper;
import com.espertech.esper.util.StringValue;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Parser for property names that can be simple, nested, mapped or a combination of these.
 * Uses ANTLR parser to parse.
 */
public class PropertyParser {
    private static final Logger log = LoggerFactory.getLogger(PropertyParser.class);

    private static Set<String> keywordCache;

    public static Property parseAndWalk(String property, boolean isRootedDynamic) {
        return walk(parse(property), isRootedDynamic);
    }

    /**
     * Parses property.
     * For cases when the property is not following the property syntax assume we act lax and assume its a simple property.
     *
     * @param property to parse
     * @return property or SimpleProperty if the property cannot be parsed
     */
    public static Property parseAndWalkLaxToSimple(String property) {
        try {
            return walk(parse(property), false);
        } catch (PropertyAccessException p) {
            return new SimpleProperty(property);
        }
    }

    /**
     * Parse the given property name returning a Property instance for the property.
     *
     * @param isRootedDynamic is true to indicate that the property is already rooted in a dynamic
     *                        property and therefore all child properties should be dynamic properties as well
     * @param tree            tree
     * @return Property instance for property
     */
    public static Property walk(EsperEPL2GrammarParser.StartEventPropertyRuleContext tree, boolean isRootedDynamic) {
        if (tree.eventProperty().eventPropertyAtomic().size() == 1) {
            return makeProperty(tree.eventProperty().eventPropertyAtomic(0), isRootedDynamic);
        }

        EsperEPL2GrammarParser.EventPropertyContext propertyRoot = tree.eventProperty();

        List<Property> properties = new LinkedList<Property>();
        boolean isRootedInDynamic = isRootedDynamic;
        for (EsperEPL2GrammarParser.EventPropertyAtomicContext atomic : propertyRoot.eventPropertyAtomic()) {
            Property property = makeProperty(atomic, isRootedInDynamic);
            if (property instanceof DynamicSimpleProperty) {
                isRootedInDynamic = true;
            }
            properties.add(property);
        }
        return new NestedProperty(properties);
    }

    /**
     * Parses a given property name returning an AST.
     *
     * @param propertyName to parse
     * @return AST syntax tree
     */
    public static EsperEPL2GrammarParser.StartEventPropertyRuleContext parse(String propertyName) {
        CharStream input = new CaseInsensitiveInputStream(propertyName);
        EsperEPL2GrammarLexer lex = ParseHelper.newLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        try {
            tokens.fill();
        } catch (RuntimeException e) {
            if (ParseHelper.hasControlCharacters(propertyName)) {
                throw new PropertyAccessException("Unrecognized control characters found in text");
            }
            throw new PropertyAccessException("Failed to parse text: " + e.getMessage());
        }

        EsperEPL2GrammarParser g = ParseHelper.newParser(tokens);
        EsperEPL2GrammarParser.StartEventPropertyRuleContext r;

        try {
            r = g.startEventPropertyRule();
        } catch (RecognitionException e) {
            return handleRecognitionEx(e, tokens, propertyName, g);
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error parsing property expression [" + propertyName + "]", e);
            }
            if (e.getCause() instanceof RecognitionException) {
                return handleRecognitionEx((RecognitionException) e.getCause(), tokens, propertyName, g);
            } else {
                throw e;
            }
        }

        return r;
    }

    private static EsperEPL2GrammarParser.StartEventPropertyRuleContext handleRecognitionEx(RecognitionException e, CommonTokenStream tokens, String propertyName, EsperEPL2GrammarParser g) {
        // Check for keywords and escape each, parse again
        String escapedPropertyName = escapeKeywords(tokens);

        CharStream inputEscaped = new CaseInsensitiveInputStream(escapedPropertyName);
        EsperEPL2GrammarLexer lexEscaped = ParseHelper.newLexer(inputEscaped);
        CommonTokenStream tokensEscaped = new CommonTokenStream(lexEscaped);
        EsperEPL2GrammarParser gEscaped = ParseHelper.newParser(tokensEscaped);

        try {
            return gEscaped.startEventPropertyRule();
        } catch (Exception eEscaped) {
        }

        throw ExceptionConvertor.convertProperty(e, propertyName, true, g);
    }

    private synchronized static String escapeKeywords(CommonTokenStream tokens) {

        if (keywordCache == null) {
            keywordCache = new HashSet<String>();
            Set<String> keywords = ParseHelper.newParser(tokens).getKeywords();
            for (String keyword : keywords) {
                if (keyword.charAt(0) == '\'' && keyword.charAt(keyword.length() - 1) == '\'') {
                    keywordCache.add(keyword.substring(1, keyword.length() - 1));
                }
            }
        }

        StringWriter writer = new StringWriter();
        // Call getTokens first before invoking tokens.size! ANTLR problem
        for (Object token : tokens.getTokens()) {
            Token t = (Token) token;
            if (t.getType() == EsperEPL2GrammarLexer.EOF) {
                break;
            }
            boolean isKeyword = keywordCache.contains(t.getText().toLowerCase(Locale.ENGLISH));
            if (isKeyword) {
                writer.append('`');
                writer.append(t.getText());
                writer.append('`');
            } else {
                writer.append(t.getText());
            }
        }
        return writer.toString();
    }

    /**
     * Returns true if the property is a dynamic property.
     *
     * @param ast property ast
     * @return dynamic or not
     */
    public static boolean isPropertyDynamic(EsperEPL2GrammarParser.StartEventPropertyRuleContext ast) {
        List<EsperEPL2GrammarParser.EventPropertyAtomicContext> ctxs = ast.eventProperty().eventPropertyAtomic();
        for (EsperEPL2GrammarParser.EventPropertyAtomicContext ctx : ctxs) {
            if (ctx.q != null || ctx.q1 != null) {
                return true;
            }
        }
        return false;
    }

    private static Property makeProperty(EsperEPL2GrammarParser.EventPropertyAtomicContext atomic, boolean isRootedInDynamic) {
        String prop = StringValue.unescapeDot(atomic.eventPropertyIdent().getText());
        if (prop.length() == 0) {
            throw new PropertyAccessException("Invalid zero-length string provided as an event property name");
        }
        if (atomic.lb != null) {
            int index = Integer.parseInt(atomic.ni.getText());
            if (!isRootedInDynamic && atomic.q == null) {
                return new IndexedProperty(prop, index);
            } else {
                return new DynamicIndexedProperty(prop, index);
            }
        } else if (atomic.lp != null) {
            String key = StringValue.parseString(atomic.s.getText());
            if (!isRootedInDynamic && atomic.q == null) {
                return new MappedProperty(prop, key);
            } else {
                return new DynamicMappedProperty(prop, key);
            }
        } else {
            if (!isRootedInDynamic && atomic.q1 == null) {
                return new SimpleProperty(prop);
            } else {
                return new DynamicSimpleProperty(prop);
            }
        }
    }

    public static String unescapeBacktickForProperty(String unescapedPropertyName) {
        if (unescapedPropertyName.startsWith("`") && unescapedPropertyName.endsWith("`")) {
            return unescapedPropertyName.substring(1, unescapedPropertyName.length() - 1);
        }

        if (!unescapedPropertyName.contains("`")) {
            return unescapedPropertyName;
        }

        // parse and render
        Property property = PropertyParser.parseAndWalkLaxToSimple(unescapedPropertyName);
        if (property instanceof NestedProperty) {
            StringWriter writer = new StringWriter();
            property.toPropertyEPL(writer);
            return writer.toString();
        }

        return unescapedPropertyName;
    }

    public static boolean isNestedPropertyWithNonSimpleLead(EsperEPL2GrammarParser.EventPropertyContext ctx) {
        if (ctx.eventPropertyAtomic().size() == 1) {
            return false;
        }
        EsperEPL2GrammarParser.EventPropertyAtomicContext atomic = ctx.eventPropertyAtomic().get(0);
        return atomic.lb != null || atomic.lp != null || atomic.q1 != null;
    }
}
