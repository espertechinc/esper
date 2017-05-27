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
package com.espertech.esper.regression.event.plugin;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventTypeException;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventPlugInInvalid implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecEventPlugInInvalid.class)) {
            return;
        }
        try {
            epService.getEPRuntime().getEventSender(new URI[0]);
            fail();
        } catch (EventTypeException ex) {
            assertEquals("Event sender for resolution URIs '[]' did not return at least one event representation's event factory", ex.getMessage());
        }
    }
}
