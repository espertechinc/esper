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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.VirtualDataWindow;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.virtualdw.VirtualDataWindowLookupContextSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDW;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDWFactory;
import junit.framework.TestCase;

import javax.naming.NamingException;
import java.util.Collections;

public class TestVirtualDataWindowToLookup extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName());
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testLateConsumerNoIterate() throws Exception {

        // client-side
        epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as SupportBean");
        SupportVirtualDW window = (SupportVirtualDW) getFromContext("/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("E1", 100);
        window.setData(Collections.singleton(supportBean));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select (select sum(intPrimitive) from MyVDW vdw where vdw.theString = s0.p00) from SupportBean_S0 s0");
        stmt.addListener(listener);
        VirtualDataWindowLookupContextSPI spiContext = (VirtualDataWindowLookupContextSPI) window.getLastRequestedIndex();

        // CM side
        epService.getEPAdministrator().createEPL("create window MyWin#unique(theString) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWin select * from SupportBean");
    }

    private VirtualDataWindow getFromContext(String name) {
        try {
            return (VirtualDataWindow) epService.getContext().lookup(name);
        }
        catch (NamingException e) {
            throw new RuntimeException("Name '" + name + "' could not be looked up");
        }
    }
}
