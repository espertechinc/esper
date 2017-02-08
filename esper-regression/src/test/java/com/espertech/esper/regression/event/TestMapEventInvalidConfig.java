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
package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Properties;

public class TestMapEventInvalidConfig extends TestCase
{
    public void testInvalidConfig()
    {
        Properties properties = new Properties();
        properties.put("astring", "XXXX");

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("MyInvalidEvent", properties);

        try
        {
            EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
            epService.initialize();
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
    }
}
