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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.ConfigurationEventTypeObjectArray;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;

public class ExecEventObjectArrayInheritanceConfigRuntime implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();
        configOps.addEventType("RootEvent", new String[]{"base"}, new Object[]{String.class});
        configOps.addEventType("Sub1Event", new String[]{"sub1"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("RootEvent")));
        configOps.addEventType("Sub2Event", new String[]{"sub2"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("RootEvent")));
        configOps.addEventType("SubAEvent", new String[]{"suba"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("Sub1Event")));
        configOps.addEventType("SubBEvent", new String[]{"subb"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("SubAEvent")));

        ExecEventObjectArrayInheritanceConfigInit.runObjectArrInheritanceAssertion(epService);
    }
}
