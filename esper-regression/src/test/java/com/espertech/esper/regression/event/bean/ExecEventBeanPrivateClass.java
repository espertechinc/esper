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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

public class ExecEventBeanPrivateClass implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + MyPrivateEvent.class.getName(),
                "Failed to resolve event type: Event class '" + MyPrivateEvent.class.getName() + "' does not have public visibility");
    }

    private static class MyPrivateEvent {
    }
}
