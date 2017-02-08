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
package com.espertech.esper.regression.datetime;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestDTInvalid extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        config.addEventType("SupportDateTime", SupportDateTime.class);
        config.addImport(SupportBean_ST0_Container.class);
        config.addPlugInSingleRowFunction("makeTest", SupportBean_ST0_Container.class.getName(), "makeTest");
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testInvalid() {
        String epl;

        // invalid incompatible params
        epl = "select contained.set('hour', 1) from SupportBean_ST0_Container";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'contained.set(\"hour\",1)': Date-time enumeration method 'set' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property but received collection of events of type '" + SupportBean_ST0.class.getName() + "' [select contained.set('hour', 1) from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select window(*).set('hour', 1) from SupportBean#keepall";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'window(*).set(\"hour\",1)': Date-time enumeration method 'set' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property but received collection of events of type 'SupportBean' [select window(*).set('hour', 1) from SupportBean#keepall]");

        // invalid incompatible params
        epl = "select utildate.set('invalid') from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.set(\"invalid\")': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value [select utildate.set('invalid') from SupportDateTime]");

        // invalid lambda parameter
        epl = "select utildate.set(x => true) from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.set()': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value [select utildate.set(x => true) from SupportDateTime]");

        // invalid no parameter
        epl = "select utildate.set() from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.set()': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value [select utildate.set() from SupportDateTime]");

        // invalid wrong parameter
        epl = "select utildate.set(1) from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.set(1)': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value [select utildate.set(1) from SupportDateTime]");

        // invalid wrong parameter
        epl = "select utildate.between('a', 'b') from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.between(\"a\",\"b\")': Error validating date-time method 'between', expected a long-typed, Date-typed or Calendar-typed result for expression parameter 0 but received java.lang.String [select utildate.between('a', 'b') from SupportDateTime]");

        // invalid wrong parameter
        epl = "select utildate.between(utildate, utildate, 1, true) from SupportDateTime";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'utildate.between(utildate,utildate,...(42 chars)': Error validating date-time method 'between', expected a boolean-type result for expression parameter 2 but received java.lang.Integer [select utildate.between(utildate, utildate, 1, true) from SupportDateTime]");
    }

    private void tryInvalid(String epl, String message) {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}
