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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultAssertExecution {
    private static final Logger log = LoggerFactory.getLogger(ResultAssertExecution.class);
    private static final Logger PREFORMATLOG = LoggerFactory.getLogger("PREFORMATTED");

    private final String epl;
    private final RegressionEnvironment env;
    private final ResultAssertTestResult expected;
    private final ResultAssertExecutionTestSelector irTestSelector;

    private static final TreeMap<Long, TimeAction> INPUT = ResultAssertInput.getActions();

    public ResultAssertExecution(String epl,
                                 RegressionEnvironment env,
                                 ResultAssertTestResult expected) {
        this(epl, env, expected, ResultAssertExecutionTestSelector.TEST_BOTH_ISTREAM_AND_IRSTREAM);
    }

    public ResultAssertExecution(String epl, RegressionEnvironment env, ResultAssertTestResult expected, ResultAssertExecutionTestSelector irTestSelector) {
        this.epl = epl;
        this.env = env;
        this.expected = expected;
        this.irTestSelector = irTestSelector;
    }

    public void execute(boolean assertAllowAnyOrder) {
        // run
        boolean isAssert = System.getProperty("ASSERTION_DISABLED") == null;
        boolean expectRemoveStream = epl.toLowerCase(Locale.ENGLISH).contains("select irstream");
        execute(isAssert, !expectRemoveStream, assertAllowAnyOrder);
        env.undeployAll();

        // Second execution is for IRSTREAM, asserting both the insert and remove stream
        if (!env.isHA()) {
            if (irTestSelector != ResultAssertExecutionTestSelector.TEST_ONLY_AS_PROVIDED) {
                String irStreamEPL = epl.replace("select ", "select irstream ");
                env.compileDeploy(irStreamEPL).addListener("s0");
                execute(isAssert, false, assertAllowAnyOrder);
                env.undeployAll();
            }
        }
    }

    private void execute(boolean isAssert, boolean isExpectNullRemoveStream, boolean assertAllowAnyOrder) {
        AtomicInteger milestone = new AtomicInteger(-1);

        // For use in join tests, send join-to events
        env.sendEventBean(new SupportBean("IBM", 0));
        env.sendEventBean(new SupportBean("MSFT", 0));
        env.sendEventBean(new SupportBean("YAH", 0));

        if (PREFORMATLOG.isDebugEnabled()) {
            PREFORMATLOG.debug(String.format("Category: %s   Output rate limiting: %s", expected.getCategory(), expected.getTitle()));
            PREFORMATLOG.debug("");
            PREFORMATLOG.debug("Statement:");
            PREFORMATLOG.debug(epl);
            PREFORMATLOG.debug("");
            PREFORMATLOG.debug(String.format("%28s  %38s", "Input", "Output"));
            PREFORMATLOG.debug(String.format("%45s  %15s  %15s", "", "Insert Stream", "Remove Stream"));
            PREFORMATLOG.debug(String.format("%28s  %30s", "-----------------------------------------------", "----------------------------------"));
            PREFORMATLOG.debug(String.format("%5s %5s%8s%8s", "Time", "Symbol", "Volume", "Price"));
        }

        for (Map.Entry<Long, TimeAction> timeEntry : INPUT.entrySet()) {
            env.milestoneInc(milestone);

            long time = timeEntry.getKey();
            String timeInSec = String.format("%3.1f", time / 1000.0);

            log.info(".execute At " + timeInSec + " sending timer event");
            sendTimer(time);

            if (PREFORMATLOG.isDebugEnabled()) {
                String comment = timeEntry.getValue().getActionDesc();
                comment = (comment == null) ? "" : comment;
                PREFORMATLOG.debug(String.format("%5s %24s %s", timeInSec, "", comment));
            }

            processAction(time, timeInSec, timeEntry.getValue(), isAssert, isExpectNullRemoveStream, assertAllowAnyOrder);
        }
    }

    private void processAction(long currentTime, String timeInSec, TimeAction value, boolean isAssert, boolean isExpectNullRemoveStream, boolean assertAllowAnyOrder) {

        Map<Integer, StepDesc> assertions = expected.getAssertions().get(currentTime);

        // Assert step 0 which is the timer event result then send events and assert remaining
        assertStep(timeInSec, 0, assertions, expected.getProperties(), isAssert, isExpectNullRemoveStream, assertAllowAnyOrder);

        for (int step = 1; step < 10; step++) {
            if (value.getEvents().size() >= step) {
                EventSendDesc sendEvent = value.getEvents().get(step - 1);
                log.info(".execute At " + timeInSec + " sending event: " + sendEvent.getTheEvent() + " " + sendEvent.getEventDesc());
                env.sendEventBean(sendEvent.getTheEvent());

                if (PREFORMATLOG.isDebugEnabled()) {
                    PREFORMATLOG.debug(String.format("%5s  %5s%8s %7.1f   %s", "",
                        sendEvent.getTheEvent().getSymbol(), sendEvent.getTheEvent().getVolume(), sendEvent.getTheEvent().getPrice(), sendEvent.getEventDesc()));
                }
            }

            assertStep(timeInSec, step, assertions, expected.getProperties(), isAssert, isExpectNullRemoveStream, assertAllowAnyOrder);
        }
    }

    private void assertStep(String timeInSec, int step, Map<Integer, StepDesc> stepAssertions, String[] fields, boolean isAssert, boolean isExpectNullRemoveStream, boolean assertAllowAnyOrder) {

        if (PREFORMATLOG.isDebugEnabled()) {
            if (env.listener("s0").isInvoked()) {
                UniformPair<String[]> received = renderReceived(fields);
                String[] newRows = received.getFirst();
                String[] oldRows = received.getSecond();
                int numMaxRows = (newRows.length > oldRows.length) ? newRows.length : oldRows.length;
                for (int i = 0; i < numMaxRows; i++) {
                    String newRow = (newRows.length > i) ? newRows[i] : "";
                    String oldRow = (oldRows.length > i) ? oldRows[i] : "";
                    PREFORMATLOG.debug(String.format("%48s %-18s %-20s", "", newRow, oldRow));
                }
                if (numMaxRows == 0) {
                    PREFORMATLOG.debug(String.format("%48s %-18s %-20s", "", "(empty result)", "(empty result)"));
                }
            }
        }

        if (!isAssert) {
            env.listener("s0").reset();
            return;
        }

        StepDesc stepDesc = null;
        if (stepAssertions != null) {
            stepDesc = stepAssertions.get(step);
        }

        // If there is no assertion, there should be no event received
        SupportListener listener = env.listener("s0");
        if (stepDesc == null) {
            Assert.assertFalse("At time " + timeInSec + " expected no events but received one or more", listener.isInvoked());
        } else {
            // If we do expect remove stream events, asset both
            if (!isExpectNullRemoveStream) {
                String message = "At time " + timeInSec;
                Assert.assertTrue(message + " expected events but received none", listener.isInvoked());
                if (assertAllowAnyOrder) {
                    EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastNewData(), expected.getProperties(),
                        stepDesc.getNewDataPerRow());
                    EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastOldData(), expected.getProperties(),
                        stepDesc.getOldDataPerRow());
                } else {
                    EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), expected.getProperties(),
                        stepDesc.getNewDataPerRow(), "newData");
                    EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), expected.getProperties(),
                        stepDesc.getOldDataPerRow(), "oldData");
                }
            } else {
                // If we don't expect remove stream events (istream only), then asset new data only if there
                // If we do expect new data, assert
                if (stepDesc.getNewDataPerRow() != null) {
                    Assert.assertTrue("At time " + timeInSec + " expected events but received none", listener.isInvoked());
                    if (assertAllowAnyOrder) {
                        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getLastNewData(), expected.getProperties(), stepDesc.getNewDataPerRow());
                    } else {
                        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), expected.getProperties(),
                            stepDesc.getNewDataPerRow(), "newData");
                    }
                } else {
                    // If we don't expect new data, make sure its null
                    Assert.assertNull("At time " + timeInSec + " expected no insert stream events but received some", listener.getLastNewData());
                }

                Assert.assertNull("At time " + timeInSec + " expected no remove stream events but received some(check irstream/istream(default) test case)", listener.getLastOldData());
            }
        }
        env.listener("s0").reset();
    }

    private UniformPair<String[]> renderReceived(String[] fields) {

        String[] renderNew = renderReceived(env.listener("s0").getNewDataListFlattened(), fields);
        String[] renderOld = renderReceived(env.listener("s0").getOldDataListFlattened(), fields);
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
        env.advanceTime(timeInMSec);
    }

    public static String getEPL(boolean join, boolean irstream, String eplNonJoin, String eplJoin) {
        String eplSelect = join ? eplJoin : eplNonJoin;
        String streamPrefix = irstream ? "select irstream" : "select";
        return "@name('s0') " + streamPrefix + " " + eplSelect;
    }

    public static String getEPL(boolean join, boolean irstream, SupportOutputLimitOpt opt, String eplNonJoin, String eplJoin) {
        return opt.getHint() + ResultAssertExecution.getEPL(join, irstream, eplNonJoin, eplJoin);
    }
}
