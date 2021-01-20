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
package com.espertech.esper.regressionlib.support.expreval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

public class SupportEvalRunner {
    private static final Logger log = LoggerFactory.getLogger(SupportEvalRunner.class);

    public static <T> SupportEvalBuilderResult run(RegressionEnvironment env, boolean soda, SupportEvalBuilder builder) {
        verifyAssertions(builder);
        SupportEvalBuilderResult result = null;
        if (!builder.isExcludeEPLAssertion()) {
            result = runEPL(env, builder, soda);
        }
        env.assertThat(() -> runNonCompile(env, builder));
        return result;
    }

    private static void verifyAssertions(SupportEvalBuilder builder) {
        for (SupportEvalAssertionPair assertion : builder.getAssertions()) {
            Map<String, SupportEvalExpected> expected = assertion.getBuilder().getResults();
            for (Map.Entry<String, String> expression : builder.getExpressions().entrySet()) {
                if (!expected.containsKey(expression.getKey())) {
                    throw new IllegalStateException("No expected value for expression '" + expression.getKey() + "'");
                }
            }
        }
    }

    private static void runNonCompile(RegressionEnvironment env, SupportEvalBuilder builder) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(builder.getEventType());
        if (eventType == null) {
            throw new IllegalArgumentException("Cannot find preconfigured event type '" + builder.getEventType() + "'");
        }
        EventType[] typesPerStream = new EventType[]{eventType};
        String[] typeAliases = new String[]{builder.getStreamAlias() == null ? "somealias" : builder.getStreamAlias()};

        Map<String, ExprEvaluator> nodes = new HashMap<>();
        for (Map.Entry<String, String> entry : builder.getExpressions().entrySet()) {
            if (builder.getExludeNamesExcept() != null && !builder.getExludeNamesExcept().equals(entry.getKey())) {
                continue;
            }
            ExprNode node = ((EPRuntimeSPI) env.runtime()).getReflectiveCompileSvc().reflectiveCompileExpression(entry.getValue(), typesPerStream, typeAliases);
            ExprEvaluator eval = node.getForge().getExprEvaluator();
            nodes.put(entry.getKey(), eval);
        }

        int count = -1;
        for (SupportEvalAssertionPair assertion : builder.getAssertions()) {
            count++;
            if (builder.getExludeAssertionsExcept() != null && count != builder.getExludeAssertionsExcept()) {
                continue;
            }
            runNonCompileAssertion(count, eventType, nodes, assertion, env, builder);
        }
    }

    private static SupportEvalBuilderResult runEPL(RegressionEnvironment env, SupportEvalBuilder builder, boolean soda) {
        StringBuilder epl = new StringBuilder();
        epl.append("@name('s0') select ");

        String delimiter = "";
        for (Map.Entry<String, String> entry : builder.getExpressions().entrySet()) {
            if (builder.getExludeNamesExcept() != null && !builder.getExludeNamesExcept().equals(entry.getKey())) {
                continue;
            }
            epl.append(delimiter);
            epl.append(entry.getValue());
            if (!entry.getValue().equals(entry.getKey())) {
                epl.append(" as ").append(entry.getKey());
            }
            delimiter = ", ";
        }

        epl.append(" from ").append(builder.getEventType());
        if (builder.getStreamAlias() != null) {
            epl.append(" as ").append(builder.getStreamAlias());
        }
        String eplText = epl.toString();
        if (builder.isLogging()) {
            log.info("EPL: " + eplText);
        }

        if (builder.getPath() != null) {
            env.compileDeploy(soda, eplText, builder.getPath()).addListener("s0");
        } else {
            env.compileDeploy(soda, eplText).addListener("s0");
        }

        if (builder.getStatementConsumer() != null && builder.getExludeAssertionsExcept() == null && builder.getExludeNamesExcept() == null) {
            env.assertStatement("s0", statement -> builder.getStatementConsumer().accept(statement));
        }

        int count = -1;
        for (SupportEvalAssertionPair assertion : builder.getAssertions()) {
            count++;
            if (builder.getExludeAssertionsExcept() != null && count != builder.getExludeAssertionsExcept()) {
                continue;
            }
            runEPLAssertion(count, builder.getEventType(), env, assertion, builder);
        }

        env.undeployModuleContaining("s0");
        return new SupportEvalBuilderResult(eplText);
    }

    private static void runEPLAssertion(int assertionNumber, String eventType, RegressionEnvironment env, SupportEvalAssertionPair assertion, SupportEvalBuilder builder) {
        if (assertion.getUnderlying() instanceof Map) {
            Map<String, Object> underlying = (Map<String, Object>) assertion.getUnderlying();
            env.sendEventMap(underlying, eventType);
            if (builder.isLogging()) {
                log.info("Sending event: " + underlying);
            }
        } else {
            env.sendEventBean(assertion.getUnderlying());
            if (builder.isLogging()) {
                log.info("Sending event: " + assertion.getUnderlying());
            }
        }

        env.assertEventNew("s0", event -> {
            if (builder.isLogging()) {
                log.info("Received event: " + EventBeanUtility.printEvent(event));
            }
            for (Map.Entry<String, SupportEvalExpected> expected : assertion.getBuilder().getResults().entrySet()) {
                String name = expected.getKey();
                if (builder.getExludeNamesExcept() != null && !builder.getExludeNamesExcept().equals(name)) {
                    continue;
                }
                Object actual = event.get(name);
                doAssert(true, assertionNumber, expected.getKey(), expected.getValue(), actual);
            }
        });
    }

    private static void runNonCompileAssertion(int assertionNumber, EventType eventType, Map<String, ExprEvaluator> nodes, SupportEvalAssertionPair assertion, RegressionEnvironment env, SupportEvalBuilder builder) {
        EventBean event;
        if (assertion.getUnderlying() instanceof Map) {
            event = new MapEventBean((Map<String, Object>) assertion.getUnderlying(), eventType);
        } else {
            if (eventType.getUnderlyingType() != assertion.getUnderlying()) {
                eventType = getSubtype(assertion.getUnderlying(), env);
            }
            event = new BeanEventBean(assertion.getUnderlying(), eventType);
        }
        EventBean[] eventsPerStream = new EventBean[]{event};

        for (Map.Entry<String, SupportEvalExpected> expected : assertion.getBuilder().getResults().entrySet()) {
            if (builder.getExludeNamesExcept() != null && !builder.getExludeNamesExcept().equals(expected.getKey())) {
                continue;
            }

            ExprEvaluator eval = nodes.get(expected.getKey());
            Object result = null;
            try {
                result = eval.evaluate(eventsPerStream, true, null);
            } catch (Throwable t) {
                log.error("Failed at expression " + expected.getKey() + " at event #" + assertionNumber, t);
                fail();
            }
            doAssert(false, assertionNumber, expected.getKey(), expected.getValue(), result);
        }
    }

    private static EventType getSubtype(Object underlying, RegressionEnvironment env) {
        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured(underlying.getClass().getSimpleName());
        if (type == null) {
            fail("Cannot find type '" + underlying.getClass().getSimpleName() + "'");
        }
        return type;
    }

    private static void doAssert(boolean epl, int assertionNumber, String columnName, SupportEvalExpected expected, Object actual) {
        String message = epl ? "For EPL assertion" : "For Eval assertion";
        message += ", failed to assert property '" + columnName + "' for event #" + assertionNumber;
        expected.assertValue(message, actual);
    }
}
