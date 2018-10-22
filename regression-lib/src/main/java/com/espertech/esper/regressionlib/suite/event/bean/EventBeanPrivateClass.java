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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

public class EventBeanPrivateClass implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        SupportMessageAssertUtil.tryInvalidCompile(env, "create schema MyPrivateEvent as " + MyPrivateEvent.class.getName(),
            "Event class '" + MyPrivateEvent.class.getName() + "' does not have public visibility");
    }

    private static class MyPrivateEvent {
    }
}
