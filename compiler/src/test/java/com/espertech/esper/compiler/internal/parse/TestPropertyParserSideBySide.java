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

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.property.*;
import com.espertech.esper.common.internal.util.StringValue;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarLexer;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import junit.framework.TestCase;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;

public class TestPropertyParserSideBySide extends TestCase {

    private static Set<String> keywordCache;

    public void testParse() {
        runAssertion("prop", new SimplePropAssertion("prop"));
        runAssertion("a[1]", new IndexedPropAssertion("a", 1));
        runAssertion("a(\"key\")", new MappedPropAssertion("a", "key"));
        runAssertion("a('key')", new MappedPropAssertion("a", "key"));
        runAssertion("a.b", new NestedPropAssertion(new SimplePropAssertion("a"), new SimplePropAssertion("b")));
        runAssertion("prop?", new SimplePropAssertion("prop", true));
        runAssertion("a[1]?", new IndexedPropAssertion("a", 1, true));
        runAssertion("a('key')?", new MappedPropAssertion("a", "key", true));
        runAssertion("item?.id", new NestedPropAssertion(new SimplePropAssertion("item", true), new SimplePropAssertion("id", true)));
        runAssertion("item[0]?.id", new NestedPropAssertion(new IndexedPropAssertion("item", 0, true), new SimplePropAssertion("id")));
        runAssertion("item('a')?.id", new NestedPropAssertion(new MappedPropAssertion("item", "a", true), new SimplePropAssertion("id")));
    }

    private void runAssertion(String expression, Consumer<Property> consumer) {
        Property antlr = antlrParseAndWalk(expression, false);
        consumer.accept(antlr);

        Property nodep = PropertyParser.parseAndWalkLaxToSimple(expression);
        consumer.accept(nodep);
    }

    public static Property antlrParseAndWalk(String property, boolean isRootedDynamic) {
        return walk(parse(property), isRootedDynamic);
    }

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

    private static class SimplePropAssertion implements Consumer<Property> {
        private final String name;
        private final boolean dynamic;

        public SimplePropAssertion(String name) {
            this(name, false);
        }

        public SimplePropAssertion(String name, boolean dynamic) {
            this.name = name;
            this.dynamic = dynamic;
        }

        public void accept(Property property) {
            if (dynamic) {
                DynamicSimpleProperty dyn = (DynamicSimpleProperty) property;
                assertEquals(name, dyn.getPropertyNameAtomic());
            } else {
                SimpleProperty prop = (SimpleProperty) property;
                assertEquals(name, prop.getPropertyNameAtomic());
            }
        }
    }

    private static class IndexedPropAssertion implements Consumer<Property> {
        private final String name;
        private final int index;
        private final boolean dynamic;

        public IndexedPropAssertion(String name, int index, boolean dynamic) {
            this.name = name;
            this.index = index;
            this.dynamic = dynamic;
        }

        public IndexedPropAssertion(String name, int index) {
            this(name, index, false);
        }

        public void accept(Property property) {
            if (dynamic) {
                DynamicIndexedProperty prop = (DynamicIndexedProperty) property;
                assertEquals(name, prop.getPropertyNameAtomic());
                assertEquals(index, prop.getIndex());
            } else {
                IndexedProperty prop = (IndexedProperty) property;
                assertEquals(name, prop.getPropertyNameAtomic());
                assertEquals(index, prop.getIndex());
            }
        }
    }

    private static class MappedPropAssertion implements Consumer<Property> {
        private final String name;
        private final String key;
        private final boolean dynamic;

        public MappedPropAssertion(String name, String key, boolean dynamic) {
            this.name = name;
            this.key = key;
            this.dynamic = dynamic;
        }

        public MappedPropAssertion(String name, String key) {
            this(name, key, false);
        }

        public void accept(Property property) {
            if (dynamic) {
                DynamicMappedProperty prop = (DynamicMappedProperty) property;
                assertEquals(name, prop.getPropertyNameAtomic());
                assertEquals(key, prop.getKey());
            } else {
                MappedProperty prop = (MappedProperty) property;
                assertEquals(name, prop.getPropertyNameAtomic());
                assertEquals(key, prop.getKey());
            }
        }
    }

    private class NestedPropAssertion implements Consumer<Property> {

        private final Consumer[] consumers;

        public NestedPropAssertion(Consumer... consumers) {
            this.consumers = consumers;
        }

        public void accept(Property property) {
            NestedProperty nested = (NestedProperty) property;
            assertEquals(consumers.length, nested.getProperties().size());
            for (int i = 0; i < nested.getProperties().size(); i++) {
                consumers[i].accept(nested.getProperties().get(i));
            }
        }
    }
}
