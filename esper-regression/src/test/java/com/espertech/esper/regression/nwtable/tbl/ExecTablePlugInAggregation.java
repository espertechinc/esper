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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static org.junit.Assert.assertEquals;

public class ExecTablePlugInAggregation implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionPlugInAggMethod_CSVLast3Strings(epService);
        runAssertionPlugInAccess_RefCountedMap(epService);
    }

    // CSV-building over a limited set of values.
    //
    // Use aggregation method single-value when the aggregation has a natural current value
    // that can be obtained without asking it a question.
    private void runAssertionPlugInAggMethod_CSVLast3Strings(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("csvWords", SimpleWordCSVFactory.class.getName());

        epService.getEPAdministrator().createEPL("create table varaggPIN (csv csvWords())");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varaggPIN.csv as c0 from SupportBean_S0").addListener(listener);
        epService.getEPAdministrator().createEPL("into table varaggPIN select csvWords(theString) as csv from SupportBean#length(3)");

        sendWordAssert(epService, listener, "the", "the");
        sendWordAssert(epService, listener, "fox", "the,fox");
        sendWordAssert(epService, listener, "jumps", "the,fox,jumps");
        sendWordAssert(epService, listener, "over", "fox,jumps,over");
    }

    // Word counting using a reference-counting-map (similar: count-min-sketch approximation, this one is more limited)
    //
    // Use aggregation access multi-value when the aggregation must be asked a specific question to return a useful value.
    private void runAssertionPlugInAccess_RefCountedMap(EPServiceProvider epService) {

        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction(
                "referenceCountedMap,referenceCountLookup".split(","), ReferenceCountedMapMultiValueFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);

        epService.getEPAdministrator().createEPL("create table varaggRCM (wordCount referenceCountedMap(string))");
        epService.getEPAdministrator().createEPL("into table varaggRCM select referenceCountedMap(theString) as wordCount from SupportBean#length(3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varaggRCM.wordCount.referenceCountLookup(p00) as c0 from SupportBean_S0").addListener(listener);

        String words = "the,house,is,green";
        sendWordAssert(epService, listener, "the", words, new Integer[]{1, null, null, null});
        sendWordAssert(epService, listener, "house", words, new Integer[]{1, 1, null, null});
        sendWordAssert(epService, listener, "the", words, new Integer[]{2, 1, null, null});
        sendWordAssert(epService, listener, "green", words, new Integer[]{1, 1, null, 1});
        sendWordAssert(epService, listener, "is", words, new Integer[]{1, null, 1, 1});
    }

    private void sendWordAssert(EPServiceProvider epService, SupportUpdateListener listener, String word, String expected) {
        epService.getEPRuntime().sendEvent(new SupportBean(word, 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void sendWordAssert(EPServiceProvider epService, SupportUpdateListener listener, String word, String wordCSV, Integer[] counts) {
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

        public AggregationFunctionFactoryCodegenType getCodegenType() {
            return AggregationFunctionFactoryCodegenType.CODEGEN_NONE;
        }
    }

    public static class SimpleWordCSVMethod implements AggregationMethod {

        private Map<String, Integer> countPerWord = new LinkedHashMap<String, Integer>();

        public void enter(Object value) {
            String word = (String) value;
            Integer count = countPerWord.get(word);
            if (count == null) {
                countPerWord.put(word, 1);
            } else {
                countPerWord.put(word, count + 1);
            }
        }

        public void leave(Object value) {
            String word = (String) value;
            Integer count = countPerWord.get(word);
            if (count == null) {
                countPerWord.put(word, 1);
            } else if (count == 1) {
                countPerWord.remove(word);
            } else {
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
        private final static AggregationStateKey SHARED_STATE_KEY = new AggregationStateKey() {
        };

        public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext) {
        }

        public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
            if (validationContext.getFunctionName().equals("referenceCountedMap")) {
                return new ReferenceCountedMapFunctionHandler(SHARED_STATE_KEY);
            }
            if (validationContext.getFunctionName().equals("referenceCountLookup")) {
                ExprEvaluator eval = validationContext.getParameterExpressions()[0].getForge().getExprEvaluator();
                return new ReferenceCountLookupFunctionHandler(SHARED_STATE_KEY, eval);
            }
            throw new IllegalArgumentException("Unexpected function name '" + validationContext.getFunctionName());
        }
    }

    public static class RefCountedMapUpdateAgentForge implements AggregationAgentForge {

        private final ExprForge forge;

        public RefCountedMapUpdateAgentForge(ExprForge forge) {
            this.forge = forge;
        }

        public AggregationAgent makeAgent(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            return new RefCountedMapUpdateAgent(forge.getExprEvaluator());
        }

        public CodegenExpression applyEnterCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
            return RefCountedMapUpdateAgent.applyEnterCodegen(forge, parent, symbols, classScope);
        }

        public CodegenExpression applyLeaveCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
            return RefCountedMapUpdateAgent.applyLeaveCodegen(forge, parent, symbols, classScope);
        }

        public ExprForge getOptionalFilter() {
            return null;
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

        public static CodegenExpression applyEnterCodegen(ExprForge forge, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
            return applyCodegen(forge, true, parent, symbols, classScope);
        }

        public static CodegenExpression applyLeaveCodegen(ExprForge forge, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
            return applyCodegen(forge, false, parent, symbols, classScope);
        }

        private static CodegenExpression applyCodegen(ExprForge forge, boolean enter, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
            CodegenMethodNode method = parent.makeChild(void.class, RefCountedMapUpdateAgent.class, classScope);
            method.getBlock().declareVar(Object.class, "value", forge.evaluateCodegen(Object.class, method, symbols, classScope))
                    .declareVar(RefCountedMapState.class, "themap", cast(RefCountedMapState.class, symbols.getAddState(method)))
                    .exprDotMethod(ref("themap"), enter ? "enter" : "leave", ref("value"));
            return localMethod(method);
        }
    }

    public static class ReferenceCountedMapFunctionHandler implements PlugInAggregationMultiFunctionHandler {
        private final AggregationStateKey sharedStateKey;

        public ReferenceCountedMapFunctionHandler(AggregationStateKey sharedStateKey) {
            this.sharedStateKey = sharedStateKey;
        }

        @Override
        public PlugInAggregationMultiFunctionCodegenType getCodegenType() {
            return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL;
        }

        public AggregationAccessorForge getAccessorForge() {
            return null;
        }

        public EPType getReturnType() {
            return EPTypeHelper.nullValue();
        }

        public AggregationStateKey getAggregationStateUniqueKey() {
            return sharedStateKey;
        }

        public PlugInAggregationMultiFunctionStateForge getStateForge() {
            return new PlugInAggregationMultiFunctionStateForge() {
                public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
                    return new PlugInAggregationMultiFunctionStateFactory() {
                        public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
                            return new RefCountedMapState();
                        }
                    };
                }
            };
        }

        public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
            return new RefCountedMapUpdateAgentForge(agentContext.getChildNodes()[0].getForge());
        }
    }

    public static class ReferenceCountLookupFunctionHandler implements PlugInAggregationMultiFunctionHandler {
        private final AggregationStateKey sharedStateKey;
        private final ExprEvaluator exprEvaluator;

        public ReferenceCountLookupFunctionHandler(AggregationStateKey sharedStateKey, ExprEvaluator exprEvaluator) {
            this.sharedStateKey = sharedStateKey;
            this.exprEvaluator = exprEvaluator;
        }

        @Override
        public PlugInAggregationMultiFunctionCodegenType getCodegenType() {
            return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL;
        }

        public AggregationAccessorForge getAccessorForge() {
            return new AggregationAccessorForge() {
                public AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
                    return new ReferenceCountLookupAccessor(exprEvaluator);
                }
            };
        }

        public EPType getReturnType() {
            return EPTypeHelper.singleValue(Integer.class);
        }

        public AggregationStateKey getAggregationStateUniqueKey() {
            return sharedStateKey;
        }

        public PlugInAggregationMultiFunctionStateForge getStateForge() {
            throw new IllegalStateException("Getter does not provide the state");
        }

        public AggregationAgentForge getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
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
            } else {
                countPerReference.put(key, count + 1);
            }
        }

        public void leave(Object key) {
            Integer count = countPerReference.get(key);
            if (count != null) {
                if (count == 1) {
                    countPerReference.remove(key);
                } else {
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


