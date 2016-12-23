/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceNotAllowedException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestIsolationUnitConfig extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
    }

    public void tearDown() {
    }

    public void testNotAllowed()
    {
        try {
            epService.getEPServiceIsolated("i1");
            fail();
        }
        catch (EPServiceNotAllowedException ex) {
            assertEquals("Isolated runtime requires execution setting to allow isolated services, please change execution settings under engine defaults", ex.getMessage());
        }
    }
}
