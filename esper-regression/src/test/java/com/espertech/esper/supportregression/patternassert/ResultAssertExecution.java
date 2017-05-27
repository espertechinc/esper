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
package com.espertech.esper.supportregression.patternassert;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.supportregression.bean.SupportBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ResultAssertExecution {
    private static final Logger log = LoggerFactory.getLogger(ResultAssertExecution.class);
    private static final Logger preformatlog = LoggerFactory.getLogger("PREFORMATTED");

    private final EPServiceProvider engine;
    private EPStatement stmt;
    private final SupportUpdateListener listener;
    private final ResultAssertTestResult expected;
    private final ResultAssertExecutionTestSelector irTestSelector;

    private static final TreeMap<Long, TimeAction> input = ResultAssertInput.getActions();

    public ResultAssertExecution(EPServiceProvider engine,
                                 EPStatement stmt,
                                 SupportUpdateListener listener,
                                 ResultAssertTestResult expected,
                                 ResultAssertExecutionTestSelector irTestSelector) {
        this.engine = engine;
        this.stmt = stmt;
        this.listener = listener;
        this.expected = expected;
        this.irTestSelector = irTestSelector;
    }

    public ResultAssertExecution(EPServiceProvider engine,
                                 EPStatement stmt,
                                 SupportUpdateListener listener,
                                 ResultAssertTestResult expected) {
        this(engine, stmt, listener, expected, ResultAssertExecutionTestSelector.TEST_BOTH_ISTREAM_AND_IRSTREAM);
    }

    public void execute(boolean allowAnyOrder) {
        boolean isAssert = System.getProperty("ASSERTION_DISABLED") == null;

        boolean expectRemoveStream = stmt.getText().toLowerCase(Locale.ENGLISH).contains("select irstream");
        execute(isAssert, !expectRemoveStream, allowAnyOrder);
        stmt.stop();

        // Second execution is for IRSTREAM, asserting both the insert and remove stream
        if (irTestSelector != ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED) {
            String epl = stmt.getText();
            String irStreamEPL = epl.replace("select ", "select irstream ");
            stmt = engine.getEPAdministrator().createEPL(irStreamEPL);
            stmt.addListener(listener);
            execute(isAssert, false, allowAnyOrder);
            stmt.stop();
        }
    }

    private void execute(boolean isAssert, boolean isExpectNullRemoveStream, boolean allowAnyOrder) {
        // For use in join tests, send join-to events
        engine.getEPRuntime().sendEvent(new SupportBean("IBM", 0));
        engine.getEPRuntime().sendEvent(new SupportBean("MSFT", 0));
        engine.getEPRuntime().sendEvent(new SupportBean("YAH", 0));

        if (preformatlog.isDebugEnabled()) {
            preformatlog.debug(String.format("Category: %s   Output rate limiting: %s", expected.getCategory(), expected.getTitle()));
            preformatlog.debug("");
            preformatlog.debug("Statement:");
            preformatlog.debug(stmt.getText());
            preformatlog.debug("");
            preformatlog.debug(String.format("%28s  %38s", "Input", "Output"));
            preformatlog.debug(String.format("%45s  %15s  %15s", "", "Insert Stream", "Remove Stream"));
            preformatlog.debug(String.format("%28s  %30s", "-----------------------------------------------", "----------------------------------"));
            preformatlog.debug(String.format("%5s %5s%8s%8s", "Time", "Symbol", "Volume", "Price"));
        }

        for (Map.Entry<Long, TimeAction> timeEntry : input.entrySet()) {
            long time = timeEntry.getKey();
            String timeInSec = String.format("%3.1f", time / 1000.0);

            log.info(".execute At " + timeInSec + " sending timer event");
            sendTimer(time);

            if (preformatlog.isDebugEnabled()) {
                String comment = timeEntry.getValue().getActionDesc();
                comment = (comment == null) ? "" : comment;
                preformatlog.debug(String.format("%5s %24s %s", timeInSec, "", comment));
            }

            processAction(time, timeInSec, timeEntry.getValue(), isAssert, isExpectNullRemoveStream, allowAnyOrder);
        }
    }

    private void processAction(long currentTime, String timeInSec, TimeAction value, boolean isAssert, boolean isExpectNullRemoveStream, boolean allowAnyOrder) {

        Map<Integer, StepDesc> assertions = expected.getAssertions().get(currentTime);

        // Assert step 0 which is the timer event result then send events and assert remaining
        assertStep(timeInSec, 0, assertions, expected.getProperties(), isAssert, isExpectNullRemoveStream, allowAnyOrder);

        for (int step = 1; step < 10; step++) {
            if (value.getEvents().size() >= step) {
                EventSendDesc sendEvent = value.getEvents().get(step - 1);
                log.info(".execute At " + timeInSec + " sending event: " + sendEvent.getTheEvent() + " " + sendEvent.getEventDesc());
                engine.getEPRuntime().sendEvent(sendEvent.getTheEvent());

                if (preformatlog.isDebugEnabled()) {
                    preformatlog.debug(String.format("%5s  %5s%8s %7.1f   %s", "",
                            sendEvent.getTheEvent().getSymbol(), sendEvent.getTheEvent().getVolume(), sendEvent.getTheEvent().getPrice(), sendEvent.getEventDesc()));
                }
            }

            assertStep(timeInSec, step, assertions, expected.getProperties(), isAssert, isExpectNullRemoveStream, allowAnyOrder);
        }
    }

    private void assertStep(String timeInSec, int step, Map<Integer, StepDesc> stepAssertions, String[] fields, boolean isAssert, boolean isExpectNullRemoveStream, boolean allowAnyOrder) {

        if (preformatlog.isDebugEnabled()) {
            if (listener.isInvoked()) {
                UniformPair<String[]> received = renderReceived(fields);
                String[] newRows = received.getFirst();
                String[] oldRows = received.getSecond();
                int numMaxRows = (newRows.length > oldRows.length) ? newRows.length : oldRows.length;
                for (int i = 0; i < numMaxRows; i++) {
                    String newRow = (newRows.length > i) ? newRows[i] : "";
                    String oldRow = (oldRows.length > i) ? oldRows[i] : "";
                    preformatlog.debug(String.format("%48s %-18s %-20s", "", newRow, oldRow));
                }
                if (numMaxRows == 0) {
                    preformatlog.debug(String.format("%48s %-18s %-20s", "", "(empty result)", "(empty result)"));
                }
            }
        }

        if (!isAssert) {
            listener.reset();
            return;
        }

        StepDesc stepDesc = null;
        if (stepAssertions != null) {
            stepDesc = stepAssertions.get(step);
        }

        // If there is no assertion, there should be no event received
        if (stepDesc == null) {
            Assert.assertFalse("At time " + timeInSec + " expected no events but received one or more", listener.isInvoked());
        } else {
            // If we do expect remove stream events, asset both
            if (!isExpectNullRemoveStream) {
                String message = "At time " + timeInSec;
                Assert.assertTrue(message + " expected events but received none", listener.isInvoked());
                if (allowAnyOrder) {
                    EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastNewData(), expected.getProperties(), stepDesc.getNewDataPerRow());
                    EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastOldData(), expected.getProperties(), stepDesc.getOldDataPerRow());
                } else {
                    EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), expected.getProperties(), stepDesc.getNewDataPerRow(), "newData");
                    EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), expected.getProperties(), stepDesc.getOldDataPerRow(), "oldData");
                }
            } else {
                // If we don't expect remove stream events (istream only), then asset new data only if there
                // If we do expect new data, assert
                if (stepDesc.getNewDataPerRow() != null) {
                    Assert.assertTrue("At time " + timeInSec + " expected events but received none", listener.isInvoked());
                    if (allowAnyOrder) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastNewData(), expected.getProperties(), stepDesc.getNewDataPerRow());
                    } else {
                        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), expected.getProperties(), stepDesc.getNewDataPerRow(), "newData");
                    }
                } else {
                    // If we don't expect new data, make sure its null
                    Assert.assertNull("At time " + timeInSec + " expected no insert stream events but received some", listener.getLastNewData());
                }

                Assert.assertNull("At time " + timeInSec + " expected no remove stream events but received some(check irstream/istream(default) test case)", listener.getLastOldData());
            }
        }
        listener.reset();
    }

    private UniformPair<String[]> renderReceived(String[] fields) {

        String[] renderNew = renderReceived(listener.getNewDataListFlattened(), fields);
        String[] renderOld = renderReceived(listener.getOldDataListFlattened(), fields);
        return new UniformPair<String[]>(renderNew, renderOld);
    }

    private String[] renderReceived(EventBean[] newDataListFlattened, String[] fields) {
        if (newDataListFlattened == null) {
            return new String[0];
        }
        String[] result = new String[newDataListFlattened.length];
        for (int i = 0; i < newDataListFlattened.length; i++) {
            Object[] values = new Object[fields.length];
            EventBean theEvent = newDataListFlattened[i];
            for (int j = 0; j < fields.length; j++) {
                values[j] = theEvent.get(fields[j]);
            }
            result[i] = Arrays.toString(values);
        }
        return result;
    }

    private void sendTimer(long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = engine.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
