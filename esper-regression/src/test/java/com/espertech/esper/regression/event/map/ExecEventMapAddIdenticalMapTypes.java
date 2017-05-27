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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;

import static org.junit.Assert.fail;

public class ExecEventMapAddIdenticalMapTypes implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        Map<String, Object> levelOne1 = ExecEventMap.makeMap(new Object[][]{{"simpleOne", Integer.class}});
        Map<String, Object> levelOne2 = ExecEventMap.makeMap(new Object[][]{{"simpleOne", Long.class}});
        Map<String, Object> levelZero1 = ExecEventMap.makeMap(new Object[][]{{"map", levelOne1}});
        Map<String, Object> levelZero2 = ExecEventMap.makeMap(new Object[][]{{"map", levelOne2}});

        // can add the same nested type twice
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero1);
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero1);
        try {
            // changing the definition however stops the compatibility
            epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero2);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }
    }
}
