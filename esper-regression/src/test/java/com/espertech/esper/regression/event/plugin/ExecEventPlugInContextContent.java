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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.plugin.PlugInEventBeanReflectorContext;
import com.espertech.esper.plugin.PlugInEventRepresentationContext;
import com.espertech.esper.plugin.PlugInEventTypeHandlerContext;
import com.espertech.esper.supportregression.event.SupportEventRepresentation;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.net.URI;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ExecEventPlugInContextContent implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addPlugInEventRepresentation(new URI("type://test/support"), SupportEventRepresentation.class.getName(), "abc");
    }

    public void run(EPServiceProvider epService) throws Exception {
        PlugInEventRepresentationContext initContext = SupportEventRepresentation.getInitContext();
        assertEquals(new URI("type://test/support"), initContext.getEventRepresentationRootURI());
        assertEquals("abc", initContext.getRepresentationInitializer());
        assertNotNull(initContext.getEventAdapterService());

        ConfigurationOperations runtimeConfig = epService.getEPAdministrator().getConfiguration();
        runtimeConfig.addPlugInEventType("TestTypeOne", new URI[]{new URI("type://test/support?a=b&c=d")}, "t1");

        PlugInEventTypeHandlerContext plugincontext = SupportEventRepresentation.getAcceptTypeContext();
        assertEquals(new URI("type://test/support?a=b&c=d"), plugincontext.getEventTypeResolutionURI());
        assertEquals("t1", plugincontext.getTypeInitializer());
        assertEquals("TestTypeOne", plugincontext.getEventTypeName());

        plugincontext = SupportEventRepresentation.getEventTypeContext();
        assertEquals(new URI("type://test/support?a=b&c=d"), plugincontext.getEventTypeResolutionURI());
        assertEquals("t1", plugincontext.getTypeInitializer());
        assertEquals("TestTypeOne", plugincontext.getEventTypeName());

        epService.getEPRuntime().getEventSender(new URI[]{new URI("type://test/support?a=b")});
        PlugInEventBeanReflectorContext contextBean = SupportEventRepresentation.getEventBeanContext();
        assertEquals("type://test/support?a=b", contextBean.getResolutionURI().toString());
    }
}
