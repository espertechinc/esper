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
package com.espertech.esper.regressionlib.support.multistmtassert;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class MultiStmtAssertUtil {

    private static final Logger log = LoggerFactory.getLogger(MultiStmtAssertUtil.class);

    public static void runIsInvokedWTestdataUniform(RegressionEnvironment env,
                                                    List<String> epls,
                                                    Object[] testData,
                                                    Consumer<Object> sender,
                                                    boolean[] received,
                                                    AtomicInteger milestone) {
        assertEquals(testData.length, received.length);
        List<EPLWithInvokedFlags> list = new ArrayList<>();
        for (String epl : epls) {
            list.add(new EPLWithInvokedFlags(epl, received));
        }
        runIsInvokedWTestdata(env, list, testData, sender, milestone);
    }

    public static void runIsInvokedWTestdata(RegressionEnvironment env,
                                             List<EPLWithInvokedFlags> descriptors,
                                             Object[] testData,
                                             Consumer<Object> sender,
                                             AtomicInteger milestone) {
        validateDescriptors(descriptors, testData.length);
        deployAndMilestone(env, descriptors, milestone);

        log.info("Running {} assertions", descriptors.size() * testData.length);
        for (int event = 0; event < testData.length; event++) {
            sender.accept(testData[event]);
            assertDescriptors(env, descriptors, event);
            env.milestone(milestone.getAndIncrement());
        }

        env.undeployAll();
    }

    public static void runIsInvokedWithEventSender(RegressionEnvironment env,
                                                   List<EPLWithInvokedFlags> descriptors,
                                                   int numEvents,
                                                   Consumer<Integer> sender,
                                                   AtomicInteger milestone) {
        validateDescriptors(descriptors, numEvents);
        deployAndMilestone(env, descriptors, milestone);

        log.info("Running {} assertions", descriptors.size() * numEvents);
        for (int event = 0; event < numEvents; event++) {
            sender.accept(event);
            assertDescriptors(env, descriptors, event);
            env.milestone(milestone.getAndIncrement());
        }

        env.undeployAll();
    }

    public static void runEPL(RegressionEnvironment env,
                              List<String> epls,
                              Object[] testData,
                              Consumer<Object> sender,
                              AsserterPerObj<String> asserter,
                              AtomicInteger milestone) {
        for (int i = 0; i < epls.size(); i++) {
            String name = "s" + i;
            String epl = "@name('" + name + "') " + epls.get(i);
            log.info("Compiling and deploying ... {}", epl);
            env.compileDeploy(epl).addListener(name);
        }

        env.milestone(milestone.getAndIncrement());

        log.info("Running {} assertions", epls.size() * testData.length);
        for (int event = 0; event < testData.length; event++) {
            sender.accept(testData[event]);

            for (int i = 0; i < epls.size(); i++) {
                String name = "s" + i;
                String epl = epls.get(i);
                String message = "Failed at event " + event + " statement + " + i + " epl ";
                asserter.assertion(event, testData[event], epl, name, message);
            }

            env.milestone(milestone.getAndIncrement());
        }

        env.undeployAll();
    }

    /**
     * For use when:
     * - small number of events to send
     * - when data and expected-output is paired already
     */
    public static void runSendAssertPairs(RegressionEnvironment env,
                                          List<String> epls,
                                          SendAssertPair[] pairs,
                                          AtomicInteger milestone) {

        for (int i = 0; i < epls.size(); i++) {
            String name = "s" + i;
            String epl = "@name('" + name + "') " + epls.get(i);
            log.info("Compiling and deploying ... {}", epl);
            env.compileDeploy(epl).addListener(name);
        }

        env.milestone(milestone.getAndIncrement());

        log.info("Running {} assertions", epls.size() * pairs.length);
        for (int event = 0; event < pairs.length; event++) {

            pairs[event].getSender().send();

            for (int i = 0; i < epls.size(); i++) {
                String name = "s" + i;
                String message = "Failed at event " + event + " statement + " + i + " epl " + epls.get(i);
                pairs[event].getAsserter().assertion(event, name, message);
            }

            env.milestone(milestone.getAndIncrement());
        }

        env.undeployAll();
    }

    private static void deployAndMilestone(RegressionEnvironment env, List<EPLWithInvokedFlags> descriptors, AtomicInteger milestone) {
        for (int i = 0; i < descriptors.size(); i++) {
            String name = "s" + i;
            EPLWithInvokedFlags desc = descriptors.get(i);
            String epl = "@name('" + name + "') " + desc.epl();
            log.info("Compiling and deploying ... {}", epl);
            env.compileDeploy(epl).addListener(name);
        }

        env.milestone(milestone.getAndIncrement());
    }

    private static void assertDescriptors(RegressionEnvironment env, List<EPLWithInvokedFlags> descriptors, int event) {
        for (int i = 0; i < descriptors.size(); i++) {
            String name = "s" + i;
            EPLWithInvokedFlags desc = descriptors.get(i);
            String message = "Failed at event " + event + " statement " + i + " epl [" + desc.epl() + "]";
            assertEquals(message, desc.getReceived()[event], env.listener(name).getAndClearIsInvoked());
        }
    }

    private static void validateDescriptors(List<EPLWithInvokedFlags> descriptors, int length) {
        for (EPLWithInvokedFlags desc : descriptors) {
            assertEquals(desc.getReceived().length, length);
        }
    }
}
