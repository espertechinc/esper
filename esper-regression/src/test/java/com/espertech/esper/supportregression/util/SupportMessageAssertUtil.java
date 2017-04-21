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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.*;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Assert;

public class SupportMessageAssertUtil {
    private static final Logger log = LoggerFactory.getLogger(SupportMessageAssertUtil.class);

    public static void tryInvalid(EPServiceProvider engine, String epl, String message) {
        try {
            engine.getEPAdministrator().createEPL(epl);
            Assert.fail();
        }
        catch (EPStatementException ex) {
            assertMessage(ex, message);
        }
    }

    public static void tryInvalidExecuteQuery(EPServiceProvider engine, String epl, String message) {
        try {
            engine.getEPRuntime().executeQuery(epl);
            Assert.fail();
        }
        catch (EPStatementException ex) {
            assertMessage(ex, message);
        }
    }

    public static void assertMessageContains(Throwable ex, String message) {
        if (!ex.getMessage().contains(message)) {
            Assert.fail("Does not contain text: '" + message + "' in text \n text:" + ex.getMessage());
        }
        if (message.trim().length() == 0) {
            ex.printStackTrace();
            Assert.fail("empty expected message");
        }
    }

    public static void assertMessage(Throwable ex, String message) {
        if (message.equals("skip")) {
            return; // skip message validation
        }
        if (message.length() > 10) {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            if (!ex.getMessage().startsWith(message)) {
                ex.printStackTrace();
                Assert.fail("\nExpected:" + message + "\nReceived:" + ex.getMessage());
            }
        }
        else {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            ex.printStackTrace();
            Assert.fail("No assertion provided, received: " + ex.getMessage());
        }
    }

    public static void tryInvalidIterate(EPServiceProvider engine, String epl, String message) {
        EPStatement stmt = engine.getEPAdministrator().createEPL(epl);
        try {
            stmt.iterator();
            Assert.fail();
        }
        catch (UnsupportedOperationException ex) {
            assertMessage(ex, message);
        }
        stmt.destroy();
    }

    public static void tryInvalidProperty(EventBean event, String propertyName) {
        try {
            event.get(propertyName);
            Assert.fail();
        }
        catch (PropertyAccessException ex) {
            // expected
            assertMessage(ex, "Property named '" + propertyName + "' is not a valid property name for this type");
        }
    }

    public static void tryInvalidGetFragment(EventBean event, String propertyName) {
        try {
            event.getFragment(propertyName);
            Assert.fail();
        }
        catch (PropertyAccessException ex) {
            // expected
            assertMessage(ex, "Property named '" + propertyName + "' is not a valid property name for this type");
        }
    }
}
