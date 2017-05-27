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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.net.URI;

import static com.espertech.esper.regression.event.plugin.ExecEventPlugInConfigRuntimeTypeResolution.configureURIs;

public class ExecEventPlugInStaticConfigDynamicTypeResolution implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configureURIs(configuration);
        URI[] uriList = new URI[]{new URI("type://properties/test2/myresolver")};
        configuration.setPlugInEventTypeResolutionURIs(uriList);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ExecEventPlugInRuntimeConfigDynamicTypeResolution.runAssertionCaseDynamic(epService);
    }
}
