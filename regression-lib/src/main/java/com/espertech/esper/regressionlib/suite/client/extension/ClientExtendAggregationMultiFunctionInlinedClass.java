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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportDeploymentDependencies;
import com.espertech.esper.regressionlib.support.util.SupportTrie;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class ClientExtendAggregationMultiFunctionInlinedClass {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendAggregationMFInlinedOneModule());
        execs.add(new ClientExtendAggregationMFInlinedOtherModule());
        return execs;
    }

    final static String INLINEDCLASS_PREFIXMAP = "inlined_class \"\"\"\n" +
        "import com.espertech.esper.common.client.*;\n" +
        "import com.espertech.esper.common.client.type.*;\n" +
        "import com.espertech.esper.common.client.hook.aggmultifunc.*;\n" +
        "import com.espertech.esper.common.client.hook.forgeinject.*;\n" +
        "import com.espertech.esper.common.internal.epl.expression.core.*;\n" +
        "import com.espertech.esper.common.internal.rettype.*;\n" +
        "import com.espertech.esper.common.internal.epl.agg.core.*;\n" +
        //
        // For use with Apache Commons Collection 4:
        //
        //"import org.apache.commons.collections4.Trie;\n" +
        //"import org.apache.commons.collections4.trie.PatriciaTrie;\n" +
        "import com.espertech.esper.regressionlib.support.util.*;\n" +
        "import java.util.*;\n" +
        "import java.util.function.*;\n" +
        "@ExtensionAggregationMultiFunction(names=\"trieState,trieEnter,triePrefixMap\")\n" +
        "    /**\n" +
        "     * The trie aggregation forge is the entry point for providing the multi-function aggregation.\n" +
        "     * This example is compatible for use with tables.\n" +
        "     */\n" +
        "    public class TrieAggForge implements AggregationMultiFunctionForge {\n" +
        "        public AggregationMultiFunctionHandler validateGetHandler(AggregationMultiFunctionValidationContext validationContext) {\n" +
        "            String name = validationContext.getFunctionName();\n" +
        "            if (name.equals(\"trieState\")) {\n" +
        "                return new TrieAggHandlerTrieState();\n" +
        "            } else if (name.equals(\"trieEnter\")) {\n" +
        "                return new TrieAggHandlerTrieEnter(validationContext.getParameterExpressions());\n" +
        "            } else if (name.equals(\"triePrefixMap\")) {\n" +
        "                return new TrieAggHandlerTriePrefixMap();\n" +
        "            }\n" +
        "            throw new IllegalStateException(\"Unrecognized name '\" + name + \"' for use with trie\");\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * This handler handles the \"trieState\"-type table column\n" +
        "         */\n" +
        "        public static class TrieAggHandlerTrieState implements AggregationMultiFunctionHandler {\n" +
        "            public EPChainableType getReturnType() {\n" +
        "                return EPChainableTypeHelper.singleValue(SupportTrie.class);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {\n" +
        "                return new AggregationMultiFunctionStateKey() {\n" +
        "                };\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateMode getStateMode() {\n" +
        "                InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(TrieAggStateFactory.class);\n" +
        "                return new AggregationMultiFunctionStateModeManaged(injection);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAccessorMode getAccessorMode() {\n" +
        "                // accessor that returns the trie itself\n" +
        "                InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(TrieAggAccessorFactory.class);\n" +
        "                return new AggregationMultiFunctionAccessorModeManaged(injection);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAgentMode getAgentMode() {\n" +
        "                throw new UnsupportedOperationException(\"Trie aggregation access is only by the 'triePrefixMap' method\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {\n" +
        "                throw new UnsupportedOperationException(\"Trie aggregation access is only by the 'triePrefixMap' method\");\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * This handler handles the \"trieEnter\"-operation that updates trie state\n" +
        "         */\n" +
        "        public static class TrieAggHandlerTrieEnter implements AggregationMultiFunctionHandler {\n" +
        "            private final ExprNode[] parameters;\n" +
        "\n" +
        "            public TrieAggHandlerTrieEnter(ExprNode[] parameters) {\n" +
        "                this.parameters = parameters;\n" +
        "            }\n" +
        "\n" +
        "            public EPChainableType getReturnType() {\n" +
        "                // We return null unless using \"prefixMap\"\n" +
        "                return EPChainableTypeNull.INSTANCE;\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {\n" +
        "                throw new UnsupportedOperationException(\"Not a trie state\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateMode getStateMode() {\n" +
        "                throw new UnsupportedOperationException(\"Not a trie state\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAccessorMode getAccessorMode() {\n" +
        "                // accessor that returns the trie itself\n" +
        "                InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(TrieAggAccessorFactory.class);\n" +
        "                return new AggregationMultiFunctionAccessorModeManaged(injection);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAgentMode getAgentMode() {\n" +
        "                if (parameters.length != 1 || ((EPTypeClass) parameters[0].getForge().getEvaluationType()).getType() != String.class) {\n" +
        "                    throw new IllegalArgumentException(\"Requires a single parameter returing a string value\");\n" +
        "                }\n" +
        "                InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(TrieAggAgentFactory.class);\n" +
        "                injection.addExpression(\"keyExpression\", parameters[0]);\n" +
        "                return new AggregationMultiFunctionAgentModeManaged(injection);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {\n" +
        "                throw new UnsupportedOperationException(\"Trie aggregation access is only by the 'triePrefixMap' method\");\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * This handler handles the \"prefixmap\" accessor for use with tables\n" +
        "         */\n" +
        "        public static class TrieAggHandlerTriePrefixMap implements AggregationMultiFunctionHandler {\n" +
        "            public EPChainableType getReturnType() {\n" +
        "                return EPChainableTypeHelper.singleValue(Map.class);\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateKey getAggregationStateUniqueKey() {\n" +
        "                throw new UnsupportedOperationException(\"Not implemented for 'triePrefixMap' trie method\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionStateMode getStateMode() {\n" +
        "                throw new UnsupportedOperationException(\"Not implemented for 'triePrefixMap' trie method\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAccessorMode getAccessorMode() {\n" +
        "                throw new UnsupportedOperationException(\"Not implemented for 'triePrefixMap' trie method\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAgentMode getAgentMode() {\n" +
        "                throw new UnsupportedOperationException(\"Not implemented for 'triePrefixMap' trie method\");\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAggregationMethodMode getAggregationMethodMode(AggregationMultiFunctionAggregationMethodContext ctx) {\n" +
        "                if (ctx.getParameters().length != 1 || ((EPTypeClass) ctx.getParameters()[0].getForge().getEvaluationType()).getType() != String.class) {\n" +
        "                    throw new IllegalArgumentException(\"Requires a single parameter returning a string value\");\n" +
        "                }\n" +
        "                InjectionStrategyClassNewInstance injection = new InjectionStrategyClassNewInstance(TrieAggMethodFactoryPrefixMap.class);\n" +
        "                injection.addExpression(\"keyExpression\", ctx.getParameters()[0]);\n" +
        "                return new AggregationMultiFunctionAggregationMethodModeManaged(injection);\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The agent state factory is responsible for producing a state holder that holds the trie state\n" +
        "         */\n" +
        "        public static class TrieAggStateFactory implements AggregationMultiFunctionStateFactory {\n" +
        "            public AggregationMultiFunctionState newState(AggregationMultiFunctionStateFactoryContext ctx) {\n" +
        "                return new TrieAggState();\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The agent state is the state holder that holds the trie state\n" +
        "         */\n" +
        "        public static class TrieAggState implements AggregationMultiFunctionState {\n" +
        "            private final SupportTrie<String, List<Object>> trie = new SupportTrieSimpleStringKeyed<>();\n" +
        "\n" +
        "            public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {\n" +
        "                throw new UnsupportedOperationException(\"Not used since the agent updates the table\");\n" +
        "            }\n" +
        "\n" +
        "            public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {\n" +
        "                throw new UnsupportedOperationException(\"Not used since the agent updates the table\");\n" +
        "            }\n" +
        "\n" +
        "            public void clear() {\n" +
        "                trie.clear();\n" +
        "            }\n" +
        "\n" +
        "            public void add(String key, Object underlying) {\n" +
        "                List<Object> existing = (List<Object>) trie.get(key);\n" +
        "                if (existing != null) {\n" +
        "                    existing.add(underlying);\n" +
        "                    return;\n" +
        "                }\n" +
        "                List<Object> events = new ArrayList<>(2);\n" +
        "                events.add(underlying);\n" +
        "                trie.put(key, events);\n" +
        "            }\n" +
        "\n" +
        "            public void remove(String key, Object underlying) {\n" +
        "                List<Object> existing = (List<Object>) trie.get(key);\n" +
        "                if (existing != null) {\n" +
        "                    existing.remove(underlying);\n" +
        "                    if (existing.isEmpty()) {\n" +
        "                        trie.remove(key);\n" +
        "                    }\n" +
        "                }\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The accessor factory is responsible for producing an accessor that returns the result of the trie table column when accessed without an aggregation method\n" +
        "         */\n" +
        "        public static class TrieAggAccessorFactory implements AggregationMultiFunctionAccessorFactory {\n" +
        "            public AggregationMultiFunctionAccessor newAccessor(AggregationMultiFunctionAccessorFactoryContext ctx) {\n" +
        "                return new TrieAggAccessor();\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The accessor returns the result of the trie table column when accessed without an aggregation method\n" +
        "         */\n" +
        "        public static class TrieAggAccessor implements AggregationMultiFunctionAccessor {\n" +
        "            // This is the value return when just referring to the trie table column by itself without a method name such as \"prefixMap\".\n" +
        "            public Object getValue(AggregationMultiFunctionState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {\n" +
        "                TrieAggState trie = (TrieAggState) state;\n" +
        "                return trie.trie;\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The agent factory is responsible for producing an agent that handles all changes to the trie table column.\n" +
        "         */\n" +
        "        public static class TrieAggAgentFactory implements AggregationMultiFunctionAgentFactory {\n" +
        "            private ExprEvaluator keyExpression;\n" +
        "\n" +
        "            public void setKeyExpression(ExprEvaluator keyExpression) {\n" +
        "                this.keyExpression = keyExpression;\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAgent newAgent(AggregationMultiFunctionAgentFactoryContext ctx) {\n" +
        "                return new TrieAggAgent(this);\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The agent is responsible for all changes to the trie table column.\n" +
        "         */\n" +
        "        public static class TrieAggAgent implements AggregationMultiFunctionAgent {\n" +
        "            private final TrieAggAgentFactory factory;\n" +
        "\n" +
        "            public TrieAggAgent(TrieAggAgentFactory factory) {\n" +
        "                this.factory = factory;\n" +
        "            }\n" +
        "\n" +
        "            public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {\n" +
        "                String key = (String) factory.keyExpression.evaluate(eventsPerStream, true, exprEvaluatorContext);\n" +
        "                TrieAggState trie = (TrieAggState) row.getAccessState(column);\n" +
        "                trie.add(key, eventsPerStream[0].getUnderlying());\n" +
        "            }\n" +
        "\n" +
        "            public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationRow row, int column) {\n" +
        "                String key = (String) factory.keyExpression.evaluate(eventsPerStream, false, exprEvaluatorContext);\n" +
        "                TrieAggState trie = (TrieAggState) row.getAccessState(column);\n" +
        "                trie.remove(key, eventsPerStream[0].getUnderlying());\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The aggregation method factory is responsible for producing an aggregation method for the \"trie\" view of the trie table column.\n" +
        "         */\n" +
        "        public static class TrieAggMethodFactoryTrieColumn implements AggregationMultiFunctionAggregationMethodFactory {\n" +
        "            public AggregationMultiFunctionAggregationMethod newMethod(AggregationMultiFunctionAggregationMethodFactoryContext context) {\n" +
        "                return new AggregationMultiFunctionAggregationMethod() {\n" +
        "                    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {\n" +
        "                        TrieAggState trie = (TrieAggState) row.getAccessState(aggColNum);\n" +
        "                        return trie.trie;\n" +
        "                    }\n" +
        "                };\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The aggregation method factory is responsible for producing an aggregation method for the \"prefixMap\" view of the trie table column.\n" +
        "         */\n" +
        "        public static class TrieAggMethodFactoryPrefixMap implements AggregationMultiFunctionAggregationMethodFactory {\n" +
        "            private ExprEvaluator keyExpression;\n" +
        "\n" +
        "            public void setKeyExpression(ExprEvaluator keyExpression) {\n" +
        "                this.keyExpression = keyExpression;\n" +
        "            }\n" +
        "\n" +
        "            public AggregationMultiFunctionAggregationMethod newMethod(AggregationMultiFunctionAggregationMethodFactoryContext context) {\n" +
        "                return new TrieAggMethodPrefixMap(this);\n" +
        "            }\n" +
        "        }\n" +
        "\n" +
        "        /**\n" +
        "         * The aggregation method is responsible for the \"prefixMap\" view of the trie table column.\n" +
        "         */\n" +
        "        public static class TrieAggMethodPrefixMap implements AggregationMultiFunctionAggregationMethod {\n" +
        "            private final TrieAggMethodFactoryPrefixMap factory;\n" +
        "\n" +
        "            public TrieAggMethodPrefixMap(TrieAggMethodFactoryPrefixMap factory) {\n" +
        "                this.factory = factory;\n" +
        "            }\n" +
        "\n" +
        "            public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {\n" +
        "                String key = (String) factory.keyExpression.evaluate(eventsPerStream, false, exprEvaluatorContext);\n" +
        "                TrieAggState trie = (TrieAggState) row.getAccessState(aggColNum);\n" +
        "                return trie.trie.prefixMap(key);\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "\"\"\"\n";

    private static class ClientExtendAggregationMFInlinedOneModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@public @buseventtype create schema PersonEvent(name string, id string);" +
                "create " + INLINEDCLASS_PREFIXMAP + ";\n" +
                "@name('table') create table TableWithTrie(nameTrie trieState(string));\n" +
                "@Priority(1) into table TableWithTrie select trieEnter(name) as nameTrie from PersonEvent;\n" +
                "@Priority(0) @name('s0') select TableWithTrie.nameTrie.triePrefixMap(name) as c0 from PersonEvent;\n";
            env.compileDeploy(epl, path).addListener("s0");

            Map<String, Object> p1 = makeSendPerson(env, "Andreas", "P1");
            assertReceived(env, CollectionUtil.buildMap("Andreas", singletonList(p1)));

            Map<String, Object> p2 = makeSendPerson(env, "Andras", "P2");
            assertReceived(env, CollectionUtil.buildMap("Andras", singletonList(p2)));

            Map<String, Object> p3 = makeSendPerson(env, "Andras", "P3");
            assertReceived(env, CollectionUtil.buildMap("Andras", Arrays.asList(p2, p3)));

            Map<String, Object> p4 = makeSendPerson(env, "And", "P4");
            assertReceived(env, CollectionUtil.buildMap("Andreas", singletonList(p1), "Andras", Arrays.asList(p2, p3), "And", singletonList(p4)));

            env.assertThat(() -> {
                String eplFAF = "select nameTrie as c0 from TableWithTrie";
                EPFireAndForgetQueryResult result = env.compileExecuteFAF(eplFAF, path);
                SupportTrie trie = (SupportTrie) result.getArray()[0].get("c0");
                assertEquals(3, trie.prefixMap("And").size());
            });

            env.assertIterator("table", iterator -> {
                SupportTrie trie = (SupportTrie) env.iterator("table").next().get("nameTrie");
                assertEquals(3, trie.prefixMap("And").size());
            });

            env.undeployAll();
        }

        private void assertReceived(RegressionEnvironment env, Map<String, Object> expected) {
            env.assertEventNew("s0", event -> {
                Map<String, List<Map<String, Object>>> received = (Map<String, List<Map<String, Object>>>) event.get("c0");
                assertEquals(expected.size(), received.size());
                for (Map.Entry<String, Object> expectedEntry : expected.entrySet()) {
                    List<Map<String, Object>> eventsExpected = (List<Map<String, Object>>) expectedEntry.getValue();
                    List<Map<String, Object>> eventsReceived = received.get(expectedEntry.getKey());
                    EPAssertionUtil.assertEqualsAllowArray("failed to compare", eventsExpected.toArray(new Map[0]), eventsReceived.toArray(new Map[0]));
                }
            });
        }
    }

    private static class ClientExtendAggregationMFInlinedOtherModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplCreateInlined = "@name('clazz') @public create " + INLINEDCLASS_PREFIXMAP + ";\n";
            RegressionPath path = new RegressionPath();
            env.compile(eplCreateInlined, path);

            String epl = "@public @buseventtype create schema PersonEvent(name string, id string);" +
                "@name('table') create table TableWithTrie(nameTrie trieState(string));\n" +
                "into table TableWithTrie select trieEnter(name) as nameTrie from PersonEvent;\n";
            EPCompiled compiledTable = env.compile(epl, path);

            env.compileDeploy(eplCreateInlined);
            env.deploy(compiledTable);

            makeSendPerson(env, "Andreas", "P1");
            makeSendPerson(env, "Andras", "P2");
            makeSendPerson(env, "Andras", "P3");
            makeSendPerson(env, "And", "P4");

            env.assertIterator("table", iterator -> {
                SupportTrie<String, List<Object>> trie = (SupportTrie<String, List<Object>>) iterator.next().get("nameTrie");
                assertEquals(3, trie.prefixMap("And").size());
            });

            // assert dependencies
            SupportDeploymentDependencies.assertSingle(env, "table", "clazz", EPObjectType.CLASSPROVIDED, "TrieAggForge");

            env.undeployAll();
        }
    }

    private static Map<String, Object> makeSendPerson(RegressionEnvironment env, String name, String id) {
        Map<String, Object> map = CollectionUtil.buildMap("name", name, "id", id);
        env.sendEventMap(map, "PersonEvent");
        return map;
    }
}
