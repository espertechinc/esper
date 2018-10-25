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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.event.variant.EventVariantStream;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestSuiteEventVariant extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventVariantStream() {
        RegressionRunner.run(session, EventVariantStream.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanVariantStream.class, SupportBeanVariantOne.class, SupportBeanVariantTwo.class,
            SupportBean_A.class, SupportBean_B.class, SupportBean_S0.class, SupportMarketDataBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> types = new HashMap<>();
        types.put("someprop", String.class);
        configuration.getCommon().addEventType("MyEvent", types);
        configuration.getCommon().addEventType("MySecondEvent", types);

        ConfigurationCommonVariantStream myVariantTwoTypedSB = new ConfigurationCommonVariantStream();
        myVariantTwoTypedSB.addEventTypeName("SupportBean");
        myVariantTwoTypedSB.addEventTypeName("SupportBeanVariantStream");
        configuration.getCommon().addVariantStream("MyVariantTwoTypedSB", myVariantTwoTypedSB);

        ConfigurationCommonVariantStream myVariantAnyTyped = new ConfigurationCommonVariantStream();
        myVariantAnyTyped.setTypeVariance(ConfigurationCommonVariantStream.TypeVariance.ANY);
        configuration.getCommon().addVariantStream("MyVariantAnyTyped", myVariantAnyTyped);
        assertTrue(configuration.getCommon().isVariantStreamExists("MyVariantAnyTyped"));

        ConfigurationCommonVariantStream myVariantTwoTyped = new ConfigurationCommonVariantStream();
        myVariantTwoTyped.addEventTypeName("MyEvent");
        myVariantTwoTyped.addEventTypeName("MySecondEvent");
        configuration.getCommon().addVariantStream("MyVariantTwoTyped", myVariantTwoTyped);

        ConfigurationCommonVariantStream myVariantTwoTypedSBVariant = new ConfigurationCommonVariantStream();
        myVariantTwoTypedSBVariant.addEventTypeName("SupportBeanVariantStream");
        myVariantTwoTypedSBVariant.addEventTypeName("SupportBean");
        configuration.getCommon().addVariantStream("MyVariantTwoTypedSBVariant", myVariantTwoTypedSBVariant);

        ConfigurationCommonVariantStream myVariantStreamTwo = new ConfigurationCommonVariantStream();
        myVariantStreamTwo.addEventTypeName("SupportBeanVariantOne");
        myVariantStreamTwo.addEventTypeName("SupportBeanVariantTwo");
        configuration.getCommon().addVariantStream("MyVariantStreamTwo", myVariantStreamTwo);

        ConfigurationCommonVariantStream myVariantStreamFour = new ConfigurationCommonVariantStream();
        myVariantStreamFour.addEventTypeName("SupportBeanVariantStream");
        myVariantStreamFour.addEventTypeName("SupportBean");
        configuration.getCommon().addVariantStream("MyVariantStreamFour", myVariantStreamFour);

        ConfigurationCommonVariantStream myVariantStreamFive = new ConfigurationCommonVariantStream();
        myVariantStreamFive.addEventTypeName("SupportBean");
        myVariantStreamFive.addEventTypeName("SupportBeanVariantStream");
        configuration.getCommon().addVariantStream("MyVariantStreamFive", myVariantStreamFive);

        ConfigurationCommonVariantStream varStreamABPredefined = new ConfigurationCommonVariantStream();
        varStreamABPredefined.addEventTypeName("SupportBean_A");
        varStreamABPredefined.addEventTypeName("SupportBean_B");
        configuration.getCommon().addVariantStream("VarStreamABPredefined", varStreamABPredefined);

        ConfigurationCommonVariantStream varStreamAny = new ConfigurationCommonVariantStream();
        varStreamAny.setTypeVariance(ConfigurationCommonVariantStream.TypeVariance.ANY);
        configuration.getCommon().addVariantStream("VarStreamAny", varStreamAny);

        // test insert into staggered with map
        ConfigurationCommonVariantStream configVariantStream = new ConfigurationCommonVariantStream();
        configVariantStream.setTypeVariance(ConfigurationCommonVariantStream.TypeVariance.ANY);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getCommon().addEventType("SupportMarketDataBean", SupportMarketDataBean.class);
        configuration.getCommon().addVariantStream("VarStreamMD", configVariantStream);

        configuration.getCommon().addImport(EventVariantStream.class);
    }
}
