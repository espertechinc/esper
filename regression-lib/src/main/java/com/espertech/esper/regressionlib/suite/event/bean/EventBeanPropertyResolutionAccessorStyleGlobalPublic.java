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
import com.espertech.esper.regressionlib.support.bean.SupportLegacyBean;

import static org.junit.Assert.assertEquals;

public class EventBeanPropertyResolutionAccessorStyleGlobalPublic implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select fieldLegacyVal from SupportLegacyBean").addListener("s0");

        SupportLegacyBean theEvent = new SupportLegacyBean("E1");
        theEvent.fieldLegacyVal = "val1";
        env.sendEventBean(theEvent);
        assertEquals("val1", env.listener("s0").assertOneGetNewAndReset().get("fieldLegacyVal"));

        env.undeployAll();
    }
}
