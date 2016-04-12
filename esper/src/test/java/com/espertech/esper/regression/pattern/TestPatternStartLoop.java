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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestPatternStartLoop extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    /**
     * Starting this statement fires an event and the listener starts a new statement (same expression) again,
     * causing a loop. This listener limits to 10 - this is a smoke test.
     */
    public void testStartFireLoop()
    {
        String patternExpr = "not " + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(patternExpr);
        patternStmt.addListener(new PatternUpdateListener());
        patternStmt.stop();
        patternStmt.start();
    }

    class PatternUpdateListener implements UpdateListener {

        private int count = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents)
        {
            log.warn(".update");

            if (count < 10)
            {
                count++;
                String patternExpr = "not " + SupportBean.class.getName();
                EPStatement patternStmt = epService.getEPAdministrator().createPattern(patternExpr);
                patternStmt.addListener(this);
                patternStmt.stop();
                patternStmt.start();
            }
        }

        public int getCount()
        {
            return count;
        }
    };

    private final static Log log = LogFactory.getLog(TestPatternStartLoop.class);
}
