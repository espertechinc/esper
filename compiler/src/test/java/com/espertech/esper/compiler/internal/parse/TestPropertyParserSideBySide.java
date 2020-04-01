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

import static com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser.*;

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
        return walk(parse(property));
    }

    public static StartEventPropertyRuleContext parse(String propertyName) {
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
        StartEventPropertyRuleContext r;

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

    private static StartEventPropertyRuleContext handleRecognitionEx(RecognitionException e, CommonTokenStream tokens, String propertyName, EsperEPL2GrammarParser g) {
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
     * @param tree            tree
     * @return Property instance for property
     */
    public static Property walk(StartEventPropertyRuleContext tree) {
        // handle root
        ChainableRootWithOptContext root = tree.chainable().chainableRootWithOpt();
        ChainableWithArgsContext rootProp = root.chainableWithArgs();

        List<ChainableAtomicWithOptContext> chained = tree.chainable().chainableElements().chainableAtomicWithOpt();
        List<Property> properties = new ArrayList<>();
        boolean optionalRoot = root.q != null;
        Property property = walkProp(rootProp, chained.isEmpty() ? null : chained.get(0), optionalRoot, false);
        properties.add(property);
        boolean rootedDynamic = property instanceof DynamicSimpleProperty;

        for (int i = 0; i < chained.size(); i++) {
            ChainableAtomicWithOptContext ctx = chained.get(i);
            if (ctx.chainableAtomic().chainableArray() != null) {
                continue;
            }
            boolean optional = ctx.q != null;
            property = walkProp(ctx.chainableAtomic().chainableWithArgs(), chained.size() <= i+1 ? null : chained.get(i + 1), optional, rootedDynamic);
            properties.add(property);
        }

        if (properties.size() == 1) {
            return properties.get(0);
        }
        return new NestedProperty(properties);
    }

    private static Property walkProp(ChainableWithArgsContext ctx, ChainableAtomicWithOptContext nextOrNull, boolean optional, boolean rootedDynamic) {
        if (nextOrNull == null) {
            return makeProperty(ctx, optional, rootedDynamic);
        }

        String name = ctx.chainableIdent().getText();
        if (nextOrNull.chainableAtomic().chainableArray() != null) {
            String indexText = nextOrNull.chainableAtomic().chainableArray().expression(0).getText();
            int index = Integer.parseInt(indexText);
            optional |= nextOrNull.q != null;
            return optional ? new DynamicIndexedProperty(name, index) : new IndexedProperty(name, index);
        }
        else {
            return makeProperty(ctx, optional, rootedDynamic);
        }
    }

    private static Property makeProperty(ChainableWithArgsContext ctx, boolean optional, boolean rootedDynamic) {
        String name = ctx.chainableIdent().getText();
        if (ctx.lp == null) {
            return optional | rootedDynamic ? new DynamicSimpleProperty(name) : new SimpleProperty(name);
        }
        LibFunctionArgItemContext func = ctx.libFunctionArgs().libFunctionArgItem().get(0);
        String key = StringValue.parseString(func.getText());
        return optional ? new DynamicMappedProperty(name, key) : new MappedProperty(name, key);
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
