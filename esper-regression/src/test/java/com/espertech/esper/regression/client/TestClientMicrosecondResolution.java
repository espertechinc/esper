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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportEngineFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class TestClientMicrosecondResolution extends TestCase {
    
    public void testClientMicrosecond() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        config.getEngineDefaults().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);

        try {
            EPServiceProviderManager.getDefaultProvider(config).initialize();
            fail();
        }
        catch (ConfigurationException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Internal timer requires millisecond time resolution");
        }

        EPServiceProvider epService = SupportEngineFactory.setupEngineDefault(TimeUnit.MICROSECONDS, 0);
        try {
            epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
            fail();
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Internal timer requires millisecond time resolution");
        }
    }
}
