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
package com.espertech.esper.regressionlib.support.patternassert;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.soda.AnnotationPart;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.apachecommonstext.StringEscapeUtils;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import junit.framework.TestCase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertNull;

/**
 * Test harness for testing expressions and comparing received MatchedEventMap instances against against expected results.
 */
public class PatternTestHarness {
    private static final Logger log = LoggerFactory.getLogger(PatternTestHarness.class);

    private final EventCollection sendEventCollection;
    private final CaseList caseList;
    private final Class testClass;

    // Array of expressions and match listeners for listening to events for each test descriptor

    public PatternTestHarness(EventCollection sendEventCollection,
                              CaseList caseList,
                              Class testClass) {
        this.sendEventCollection = sendEventCollection;
        this.caseList = caseList;
        this.testClass = testClass;
    }

    public void runTest(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();
        if (!env.isHA()) {
            runTest(env, PatternTestStyle.USE_EPL, milestone);
            runTest(env, PatternTestStyle.COMPILE_TO_MODEL, milestone);
            runTest(env, PatternTestStyle.COMPILE_TO_EPL, milestone);
            runTest(env, PatternTestStyle.USE_EPL_AND_CONSUME_NOCHECK, milestone);
        } else {
            runTest(env, PatternTestStyle.USE_EPL, milestone);
            runTest(env, PatternTestStyle.COMPILE_TO_EPL, milestone);
        }
    }

    private void runTest(RegressionEnvironment env, PatternTestStyle testStyle, AtomicInteger milestone) {

        // Send the start time to the eventService
        if (sendEventCollection.getTime(EventCollection.ON_START_EVENT_ID) != null) {
            long startTime = sendEventCollection.getTime(EventCollection.ON_START_EVENT_ID);
            env.advanceTime(startTime);
            log.debug(".runTest Start time is " + startTime);
        }

        // Set up expression filters and match listeners
        String[] expressions = new String[caseList.getNumTests()];
        int index = -1;
        for (EventExpressionCase descriptor : caseList.getResults()) {
            index++;
            String epl = descriptor.getExpressionText();
            EPStatementObjectModel model = descriptor.getObjectModel();
            String statementName = nameOfStatement(descriptor);
            String nameAnnotation = "@name(\"" + statementName + "\") ";
            EPCompiled compiled;
            log.debug(".runTest Deploying " + epl);

            try {
                if (model != null) {
                    model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation(statementName)));
                    Module module = new Module();
                    module.getItems().add(new ModuleItem(model));
                    compiled = EPCompilerProvider.getCompiler().compile(module, new CompilerArguments(env.getConfiguration()));
                } else {
                    if (testStyle == PatternTestStyle.USE_EPL) {
                        String text = nameAnnotation + "@Audit('pattern') @Audit('pattern-instances') select * from pattern [" + epl + "]";
                        compiled = env.compile(text);
                        epl = text;
                    } else if (testStyle == PatternTestStyle.USE_EPL_AND_CONSUME_NOCHECK) {
                        String text = nameAnnotation + "select * from pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [" + epl + "]";
                        compiled = env.compile(text);
                        epl = text;
                    } else if (testStyle == PatternTestStyle.COMPILE_TO_MODEL) {
                        String text = nameAnnotation + "select * from pattern [" + epl + "]";
                        EPStatementObjectModel mymodel = EPCompilerProvider.getCompiler().eplToModel(text, env.getConfiguration());
                        Module module = new Module();
                        module.getItems().add(new ModuleItem(mymodel));
                        compiled = EPCompilerProvider.getCompiler().compile(module, new CompilerArguments(env.getConfiguration()));
                        epl = text;
                    } else if (testStyle == PatternTestStyle.COMPILE_TO_EPL) {
                        String text = "select * from pattern [" + epl + "]";
                        EPStatementObjectModel mymodel = EPCompilerProvider.getCompiler().eplToModel(text, env.getConfiguration());
                        String reverse = nameAnnotation + mymodel.toEPL();
                        compiled = env.compile(reverse);
                        epl = reverse;
                    } else {
                        throw new IllegalArgumentException("Unknown test style");
                    }
                }
            } catch (Exception ex) {
                String text = epl;
                if (model != null) {
                    text = "Model: " + model.toEPL();
                }
                log.error(".runTest Failed to create statement for style " + testStyle + " pattern expression=" + text, ex);
                TestCase.fail();
                compiled = null;
            }

            // We stop the statement again and start after the first listener was added.
            // Thus we can handle patterns that fireStatementStopped on startup.
            final EPCompiled unit = compiled;
            env.deploy(unit).addListener(statementName);
            expressions[index] = epl;
        }

        // milestone
        env.milestone(milestone.getAndIncrement());

        // Some expressions may fireStatementStopped as soon as they are started, such as a "not b()" expression, for example.
        // Check results for any such listeners/expressions.
        // NOTE: For EPL statements we do not support calling listeners when a pattern that fires upon start.
        // Reason is that this should not be a relevant functionality of a pattern, the start pattern
        // event itself cannot carry any information and is thus ignore. Note subsequent events
        // generated by the same pattern are fine.
        checkResults(testStyle, EventCollection.ON_START_EVENT_ID, expressions, env);
        int totalEventsReceived = countExpectedEvents(EventCollection.ON_START_EVENT_ID);
        clearListenerEvents(caseList, env);

        // Send actual test events
        for (Map.Entry<String, Object> entry : sendEventCollection.entrySet()) {
            String eventId = entry.getKey();

            // Manipulate the time when this event was send
            if (sendEventCollection.getTime(eventId) != null) {
                long currentTime = sendEventCollection.getTime(eventId);
                env.advanceTime(currentTime);
                log.debug(".runTest Sending event " + entry.getKey()
                    + " = " + entry.getValue() +
                    "  timed " + currentTime);
            }

            // Send event itself
            env.sendEventBean(entry.getValue());

            // Check expected results for this event
            if (testStyle != PatternTestStyle.USE_EPL_AND_CONSUME_NOCHECK) {
                checkResults(testStyle, eventId, expressions, env);

                // Count and clear the list of events that each listener has received
                totalEventsReceived += countListenerEvents(caseList, env);
            }
            clearListenerEvents(caseList, env);

            env.milestone(milestone.getAndIncrement());
        }

        // Count number of expected matches
        int totalExpected = 0;
        for (EventExpressionCase descriptor : caseList.getResults()) {
            for (LinkedList<EventDescriptor> events : descriptor.getExpectedResults().values()) {
                totalExpected += events.size();
            }
        }

        if (totalExpected != totalEventsReceived && testStyle != PatternTestStyle.USE_EPL_AND_CONSUME_NOCHECK) {
            log.debug(".test Count expected does not match count received, expected=" + totalExpected +
                " received=" + totalEventsReceived);
            TestCase.assertTrue(false);
        }

        // Kill all expressions
        env.undeployAll();

        // Send test events again to also test that all were indeed killed
        for (Map.Entry<String, Object> entry : sendEventCollection.entrySet()) {
            env.sendEventBean(entry.getValue());
        }

        // Make sure all listeners are still at zero
        for (EventExpressionCase descriptor : caseList.getResults()) {
            String statementName = nameOfStatement(descriptor);
            assertNull(env.statement(statementName));
        }
    }

    private void checkResults(PatternTestStyle testStyle, String eventId, String[] expressions, RegressionEnvironment env) {
        // For each test descriptor, make sure the listener has received exactly the events expected
        int index = 0;
        log.debug(".checkResults Checking results for event " + eventId);

        for (EventExpressionCase descriptor : caseList.getResults()) {
            String statementName = nameOfStatement(descriptor);
            String expressionText = expressions[index];

            LinkedHashMap<String, LinkedList<EventDescriptor>> allExpectedResults = descriptor.getExpectedResults();
            SupportListener listener = env.listener(statementName);
            EventBean[] receivedResults = listener.getLastNewData();
            index++;

            // If nothing at all was expected for this event, make sure nothing was received
            if (!(allExpectedResults.containsKey(eventId))) {
                if ((receivedResults != null) && (receivedResults.length > 0)) {
                    log.debug(".checkResults Incorrect result for style " + testStyle + " expression : " + expressionText);
                    log.debug(".checkResults Expected no results for event " + eventId + ", but received " + receivedResults.length + " events");
                    log.debug(".checkResults Received, have " + receivedResults.length + " entries");
                    printList(receivedResults);
                    TestCase.assertFalse(true);
                }
                continue;
            }

            LinkedList<EventDescriptor> expectedResults = allExpectedResults.get(eventId);

            // Compare the result lists, not caring about the order of the elements
            try {
                if (!(compareLists(receivedResults, expectedResults))) {
                    log.debug(".checkResults Incorrect result for style " + testStyle + " expression : " + expressionText);
                    log.debug(".checkResults Expected size=" + expectedResults.size() + " received size=" + (receivedResults == null ? 0 : receivedResults.length));

                    log.debug(".checkResults Expected, have " + expectedResults.size() + " entries");
                    printList(expectedResults);
                    log.debug(".checkResults Received, have " + (receivedResults == null ? 0 : receivedResults.length) + " entries");
                    printList(receivedResults);

                    TestCase.assertFalse(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail("For statement '" + expressionText + "' failed to assert: " + ex.getMessage());
            }
        }
    }

    private boolean compareLists(EventBean[] receivedResults,
                                 LinkedList<EventDescriptor> expectedResults) {
        int receivedSize = (receivedResults == null) ? 0 : receivedResults.length;
        if (expectedResults.size() != receivedSize) {
            return false;
        }

        // To make sure all received events have been expected
        LinkedList<EventDescriptor> expectedResultsClone = new LinkedList<EventDescriptor>(expectedResults);

        // Go through the list of expected results and remove from received result list if found
        for (EventDescriptor desc : expectedResults) {
            EventDescriptor foundMatch = null;

            for (EventBean received : receivedResults) {
                if (compareEvents(desc, received)) {
                    foundMatch = desc;
                    break;
                }
            }

            // No match between expected and received
            if (foundMatch == null) {
                return false;
            }

            expectedResultsClone.remove(foundMatch);
        }

        // Any left over received results also invalidate the test
        if (expectedResultsClone.size() > 0) {
            return false;
        }
        return true;
    }

    private static boolean compareEvents(EventDescriptor eventDesc, EventBean eventBean) {
        for (Map.Entry<String, Object> entry : eventDesc.getEventProperties().entrySet()) {
            Object result = eventBean.get(entry.getKey());
            if ((result == null) && (entry.getValue() == null)) {
                continue;
            }
            if (result == null) {
                log.debug("For tag " + entry.getKey() + " the value is NULL");
                return false;
            }
            if (!result.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear the event list of all listeners
     *
     * @param caseList
     * @param env
     */
    private int countListenerEvents(CaseList caseList, RegressionEnvironment env) {
        int count = 0;
        for (EventExpressionCase descriptor : caseList.getResults()) {
            String statementName = nameOfStatement(descriptor);
            for (EventBean[] events : env.listener(statementName).getNewDataList()) {
                count += events.length;
            }
        }
        return count;
    }

    private void clearListenerEvents(CaseList caseList, RegressionEnvironment env) {
        for (EventExpressionCase descriptor : caseList.getResults()) {
            String statementName = nameOfStatement(descriptor);
            env.listener(statementName).reset();
        }
    }

    private void printList(LinkedList<EventDescriptor> events) {
        int index = 0;
        for (EventDescriptor desc : events) {
            StringBuilder buffer = new StringBuilder();
            int count = 0;

            for (Map.Entry<String, Object> entry : desc.getEventProperties().entrySet()) {
                buffer.append(" (" + (count++) + ") ");
                buffer.append("tag=" + entry.getKey());

                String id = findValue(entry.getValue());
                buffer.append("  eventId=" + id);
            }

            log.debug(".printList (" + index + ") : " + buffer.toString());
            index++;
        }
    }

    private void printList(EventBean[] events) {
        if (events == null) {
            log.debug(".printList : null-value events array");
            return;
        }

        log.debug(".printList : " + events.length + " elements...");
        for (int i = 0; i < events.length; i++) {
            log.debug("  " + EventBeanUtility.printEvent(events[i]));
        }
    }

    /**
     * Find the value object in the map of object names and values
     */
    private String findValue(Object value) {
        for (Map.Entry<String, Object> entry : sendEventCollection.entrySet()) {
            if (value == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private int countExpectedEvents(String eventId) {
        int result = 0;
        for (EventExpressionCase descriptor : caseList.getResults()) {
            LinkedHashMap<String, LinkedList<EventDescriptor>> allExpectedResults = descriptor.getExpectedResults();

            // If nothing at all was expected for this event, make sure nothing was received
            if (allExpectedResults.containsKey(eventId)) {
                result++;
            }
        }
        return result;
    }

    private enum PatternTestStyle {
        USE_EPL,
        COMPILE_TO_MODEL,
        COMPILE_TO_EPL,
        USE_EPL_AND_CONSUME_NOCHECK,
    }

    private String nameOfStatement(EventExpressionCase descriptor) {
        return "name--" + StringEscapeUtils.escapeJava(descriptor.getExpressionText());
    }
}
