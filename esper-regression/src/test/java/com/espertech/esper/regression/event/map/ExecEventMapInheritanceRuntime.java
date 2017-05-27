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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;

public class ExecEventMapInheritanceRuntime implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        Map<String, Object> root = ExecEventMap.makeMap(new Object[][]{{"base", String.class}});
        Map<String, Object> sub1 = ExecEventMap.makeMap(new Object[][]{{"sub1", String.class}});
        Map<String, Object> sub2 = ExecEventMap.makeMap(new Object[][]{{"sub2", String.class}});
        Map<String, Object> suba = ExecEventMap.makeMap(new Object[][]{{"suba", String.class}});
        Map<String, Object> subb = ExecEventMap.makeMap(new Object[][]{{"subb", String.class}});

        epService.getEPAdministrator().getConfiguration().addEventType("RootEvent", root);
        epService.getEPAdministrator().getConfiguration().addEventType("Sub1Event", sub1, new String[]{"RootEvent"});
        epService.getEPAdministrator().getConfiguration().addEventType("Sub2Event", sub2, new String[]{"RootEvent"});
        epService.getEPAdministrator().getConfiguration().addEventType("SubAEvent", suba, new String[]{"Sub1Event"});
        epService.getEPAdministrator().getConfiguration().addEventType("SubBEvent", subb, new String[]{"Sub1Event", "Sub2Event"});

        ExecEventMapInheritanceInitTime.runAssertionMapInheritance(epService);
    }
}
