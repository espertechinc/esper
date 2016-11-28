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

package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.*;
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRowPatternRecognitionIterateOnly extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(TestRowPatternRecognitionIterateOnly.class);

    public void testNoListenerMode()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        config.addImport(SupportStaticMethodLib.class.getName());
        config.addImport(Hint.class.getName());
        config.addVariable("mySleepDuration", long.class, 100);    // msec
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String[] fields = "a".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#length(1) " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as SupportStaticMethodLib.sleepReturnTrue(mySleepDuration)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // this should not block
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        }
        long end = System.currentTimeMillis();
        assertTrue((end - start) <= 100);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        epService.getEPRuntime().setVariableValue("mySleepDuration", 0);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2"}});
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testPrev()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String[] fields = "a".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#lastevent " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 2));
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E7"}});
        assertFalse(listener.isInvoked());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testPrevPartitioned()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String[] fields = "a,cat".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#lastevent " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.theString as a, A.cat as cat" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", "A", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", "B", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", "B", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", "A", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", "B", 2));
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", "A", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E6", "A"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", "B", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E7", "B"}});
        assertFalse(listener.isInvoked());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
}
