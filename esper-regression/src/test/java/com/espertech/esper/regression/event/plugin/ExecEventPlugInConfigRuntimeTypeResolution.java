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
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.plugineventrep.MyPlugInEventRepresentation;

import java.net.URI;

public class ExecEventPlugInConfigRuntimeTypeResolution implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configureURIs(configuration);
    }

    public static void configureURIs(Configuration configuration) throws Exception {
        configuration.addPlugInEventRepresentation(new URI("type://properties"), MyPlugInEventRepresentation.class.getName(), "r3");
        configuration.addPlugInEventRepresentation(new URI("type://properties/test1"), MyPlugInEventRepresentation.class.getName(), "r1");
        configuration.addPlugInEventRepresentation(new URI("type://properties/test2"), MyPlugInEventRepresentation.class.getName(), "r2");
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecEventPlugInConfigRuntimeTypeResolution.class)) {
            return;
        }
        ConfigurationOperations ops = epService.getEPAdministrator().getConfiguration();
        ops.addPlugInEventType("TestTypeOne", new URI[]{new URI("type://properties/test1/testtype")}, "t1");
        ops.addPlugInEventType("TestTypeTwo", new URI[]{new URI("type://properties/test2")}, "t2");
        ops.addPlugInEventType("TestTypeThree", new URI[]{new URI("type://properties/test3")}, "t3");
        ops.addPlugInEventType("TestTypeFour", new URI[]{new URI("type://properties/test2/x"), new URI("type://properties/test3")}, "t4");

        ExecEventPlugInConfigStaticTypeResolution.runAssertionCaseStatic(epService);
    }
}
