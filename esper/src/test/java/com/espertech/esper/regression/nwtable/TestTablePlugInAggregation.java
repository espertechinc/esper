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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.plugin.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestTablePlugInAggregation extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    // CSV-building over a limited set of values.
    //
    // Use aggregation method single-value when the aggregation has a natural current value
    // that can be obtained without asking it a question.
    public void testPlugInAggMethod_CSVLast3Strings() {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("csvWords", SimpleWordCSVFactory.class.getName());

        epService.getEPAdministrator().createEPL("create table varagg (csv csvWords())");
        epService.getEPAdministrator().createEPL("select varagg.csv as c0 from SupportBean_S0").addListener(listener);
        epService.getEPAdministrator().createEPL("into table varagg select csvWords(theString) as csv from SupportBean#length(3)");

        sendWordAssert("the", "the");
        sendWordAssert("fox", "the,fox");
        sendWordAssert("jumps", "the,fox,jumps");
        sendWordAssert("over", "fox,jumps,over");
    }

    // Word counting using a reference-counting-map (similar: count-min-sketch approximation, this one is more limited)
    //
    // Use aggregation access multi-value when the aggregation must be asked a specific question to return a useful value.
    public void testPlugInAccess_RefCountedMap() {

        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction(
                "referenceCountedMap,referenceCountLookup".split(","), ReferenceCountedMapMultiValueFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);

        epService.getEPAdministrator().createEPL("create table varagg (wordCount referenceCountedMap(string))");
        epService.getEPAdministrator().createEPL("into table varagg select referenceCountedMap(theString) as wordCount from SupportBean#length(3)");
        epService.getEPAdministrator().createEPL("select varagg.wordCount.referenceCountLookup(p00) as c0 from SupportBean_S0").addListener(listener);

        String words = "the,house,is,green";
        sendWordAssert("the", words, new Integer[]{1, null, null, null});
        sendWordAssert("house", words, new Integer[]{1, 1, null, null});
        sendWordAssert("the", words, new Integer[]{2, 1, null, null});
        sendWordAssert("green", words, new Integer[]{1, 1, null, 1});
        sendWordAssert("is", words, new Integer[]{1, null, 1, 1});
    }

    private void sendWordAssert(String word, String expected) {
        epService.getEPRuntime().sendEvent(new SupportBean(word, 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void sendWordAssert(String word, String wordCSV, Integer[] counts) {
        epService.getEPRuntime().sendEvent(new SupportBean(word, 0));

        String[] words = wordCSV.split(",");
        for (int i = 0; i < words.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, words[i]));
            Integer count = (Integer) listener.assertOneGetNewAndReset().get("c0");
            assertEquals("failed for word '" + words[i] + "'", counts[i], count);
        }
    }

    public static class SimpleWordCSVFactory implements AggregationFunctionFactory {
        public void setFunctionName(String functionName) {
        }

        public void validate(AggregationValidationContext validationContext) {
        }

        public AggregationMethod newAggregator() {
            return new SimpleWordCSVMethod();
        }

        public Class getValueType() {
            return String.class;
        }
    }

    public static class SimpleWordCSVMethod implements AggregationMethod {

        private Map<String, Integer> countPerWord = new LinkedHashMap<String, Integer>();

        public void enter(Object value) {
            String word = (String) value;
            Integer count = countPerWord.get(word);
            if (count == null) {
                countPerWord.put(word, 1);
            }
            else {
                countPerWord.put(word, count + 1);
            }
        }

        public void leave(Object value) {
            String word = (String) value;
            Integer count = countPerWord.get(word);
            if (count == null) {
                countPerWord.put(word, 1);
            }
            else if (count == 1) {
                countPerWord.remove(word);
            }
            else {
                countPerWord.put(word, count - 1);
            }
        }

        public Object getValue() {
            StringWriter writer = new StringWriter();
            String delimiter = "";
            for (Map.Entry<String, Integer> entry : countPerWord.entrySet()) {
                writer.append(delimiter);
                delimiter = ",";
                writer.append(entry.getKey());
            }
            return writer.toString();
        }

        public void clear() {
            countPerWord.clear();
        }
    }

    public static class ReferenceCountedMapMultiValueFactory implements PlugInAggregationMultiFunctionFactory {
        private final static AggregationStateKey sharedStateKey = new AggregationStateKey() {};

        public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext) {
        }

        public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
            if (validationContext.getFunctionName().equals("referenceCountedMap")) {
                return new ReferenceCountedMapFunctionHandler(sharedStateKey);
            }
            if (validationContext.getFunctionName().equals("referenceCountLookup")) {
                ExprEvaluator eval = validationContext.getParameterExpressions()[0].getExprEvaluator();
                return new ReferenceCountLookupFunctionHandler(sharedStateKey, eval);
            }
            throw new IllegalArgumentException("Unexpected function name '" + validationContext.getFunctionName());
        }
    }

    public static class RefCountedMapUpdateAgent implements AggregationAgent {

        private final ExprEvaluator evaluator;

        public RefCountedMapUpdateAgent(ExprEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
            Object value = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            RefCountedMapState themap = (RefCountedMapState) aggregationState;
            themap.enter(value);
        }

        public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
            Object value = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            RefCountedMapState themap = (RefCountedMapState) aggregationState;
            themap.leave(value);
        }
    }

    public static class ReferenceCountedMapFunctionHandler implements PlugInAggregationMultiFunctionHandler {
        private final AggregationStateKey sharedStateKey;

        public ReferenceCountedMapFunctionHandler(AggregationStateKey sharedStateKey) {
            this.sharedStateKey = sharedStateKey;
        }

        public AggregationAccessor getAccessor() {
            return null;
        }

        public EPType getReturnType() {
            return EPTypeHelper.nullValue();
        }

        public AggregationStateKey getAggregationStateUniqueKey() {
            return sharedStateKey;
        }

        public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
            return new PlugInAggregationMultiFunctionStateFactory() {
                public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
                    return new RefCountedMapState();
                }
            };
        }

        public AggregationAgent getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
            return new RefCountedMapUpdateAgent(agentContext.getChildNodes()[0].getExprEvaluator());
        }
    }

    public static class ReferenceCountLookupFunctionHandler implements PlugInAggregationMultiFunctionHandler {
        private final AggregationStateKey sharedStateKey;
        private final ExprEvaluator exprEvaluator;

        public ReferenceCountLookupFunctionHandler(AggregationStateKey sharedStateKey, ExprEvaluator exprEvaluator) {
            this.sharedStateKey = sharedStateKey;
            this.exprEvaluator = exprEvaluator;
        }

        public AggregationAccessor getAccessor() {
            return new ReferenceCountLookupAccessor(exprEvaluator);
        }

        public EPType getReturnType() {
            return EPTypeHelper.singleValue(Integer.class);
        }

        public AggregationStateKey getAggregationStateUniqueKey() {
            return sharedStateKey;
        }

        public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
            throw new IllegalStateException("Getter does not provide the state");
        }

        public AggregationAgent getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
            return null;
        }
    }

    public static class ReferenceCountLookupAccessor implements AggregationAccessor {

        private final ExprEvaluator exprEvaluator;

        public ReferenceCountLookupAccessor(ExprEvaluator exprEvaluator) {
            this.exprEvaluator = exprEvaluator;
        }

        public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            RefCountedMapState mystate = (RefCountedMapState) state;
            Object lookupKey = exprEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            return mystate.getCountPerReference().get(lookupKey);
        }

        public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return null;
        }

        public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return null;
        }

        public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return null;
        }
    }

    public static class RefCountedMapState implements AggregationState {

        private final Map<Object, Integer> countPerReference = new LinkedHashMap<Object, Integer>();

        public Map<Object, Integer> getCountPerReference() {
            return countPerReference;
        }

        public void enter(Object key) {
            Integer count = countPerReference.get(key);
            if (count == null) {
                countPerReference.put(key, 1);
            }
            else {
                countPerReference.put(key, count + 1);
            }
        }

        public void leave(Object key) {
            Integer count = countPerReference.get(key);
            if (count != null) {
                if (count == 1) {
                    countPerReference.remove(key);
                }
                else {
                    countPerReference.put(key, count - 1);
                }
            }
        }

        public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
            // no need to implement, we mutate using enter and leave instead
            throw new UnsupportedOperationException("Use enter instead");
        }

        public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
            // no need to implement, we mutate using enter and leave instead
            throw new UnsupportedOperationException("Use leave instead");
        }

        public void clear() {
            countPerReference.clear();
        }
    }
}


