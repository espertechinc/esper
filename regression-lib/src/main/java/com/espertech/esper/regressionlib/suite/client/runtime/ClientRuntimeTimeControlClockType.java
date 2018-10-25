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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadSleep;
import static org.junit.Assert.*;

public class ClientRuntimeTimeControlClockType {

    public void run(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addEventType(SupportBean.class);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(ClientRuntimeTimeControlClockType.class.getSimpleName(), configuration);

        runtime.getEventService().advanceTime(0);
        assertEquals(0, runtime.getEventService().getCurrentTime());
        assertTrue(runtime.getEventService().isExternalClockingEnabled());

        runtime.getEventService().clockInternal();
        assertFalse(runtime.getEventService().isExternalClockingEnabled());
        long waitStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - waitStart < 10000) {
            if (runtime.getEventService().getCurrentTime() > 0) {
                break;
            }
        }
        assertNotEquals(0, runtime.getEventService().getCurrentTime());
        assertTrue(System.currentTimeMillis() > runtime.getEventService().getCurrentTime() - 10000);

        runtime.getEventService().clockExternal();
        assertTrue(runtime.getEventService().isExternalClockingEnabled());
        runtime.getEventService().advanceTime(0);
        threadSleep(500);
        assertEquals(0, runtime.getEventService().getCurrentTime());

        runtime.destroy();
    }
}
