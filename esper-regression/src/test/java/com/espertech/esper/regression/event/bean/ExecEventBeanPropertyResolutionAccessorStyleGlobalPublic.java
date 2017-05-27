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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportLegacyBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEventBeanPropertyResolutionAccessorStyleGlobalPublic implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        configuration.addEventType("SupportLegacyBean", SupportLegacyBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select fieldLegacyVal from SupportLegacyBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportLegacyBean theEvent = new SupportLegacyBean("E1");
        theEvent.fieldLegacyVal = "val1";
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals("val1", listener.assertOneGetNewAndReset().get("fieldLegacyVal"));
    }
}
