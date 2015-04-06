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

package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestAPIConfigAndInstance extends TestCase {

    private EPServiceProvider epService;
    private EPDataFlowRuntime dataFlowRuntime;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        dataFlowRuntime = epService.getEPRuntime().getDataFlowRuntime();
        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        dataFlowRuntime = null;
        epService = null;
        listener = null;
    }

    public void testDataFlowConfigAndInstance() throws Exception {
        assertEquals(0, dataFlowRuntime.getSavedConfigurations().length);
        assertNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));
        assertFalse(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        try {
            dataFlowRuntime.instantiateSavedConfiguration("MyFirstFlow");
            fail();
        }
        catch(EPDataFlowInstantiationException ex) {
            assertEquals("Dataflow saved configuration 'MyFirstFlow' could not be found", ex.getMessage());
        }
        try {
            dataFlowRuntime.saveConfiguration("MyFirstFlow", "MyDataflow", null);
            fail();
        }
        catch(EPDataFlowNotFoundException ex) {
            assertEquals("Failed to locate data flow 'MyDataflow'", ex.getMessage());
        }

        // finally create one
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataflow " +
                "BeaconSource -> outdata<MyEvent> {" +
                "  iterations:1" +
                "}" +
                "EventBusSink(outdata) {}");

        // add it
        dataFlowRuntime.saveConfiguration("MyFirstFlow", "MyDataflow", null);
        assertEquals(1, dataFlowRuntime.getSavedConfigurations().length);
        EPDataFlowSavedConfiguration savedConfiguration = dataFlowRuntime.getSavedConfiguration(dataFlowRuntime.getSavedConfigurations()[0]);
        assertEquals("MyFirstFlow", savedConfiguration.getSavedConfigurationName());
        assertEquals("MyDataflow", savedConfiguration.getDataflowName());
        try {
            dataFlowRuntime.saveConfiguration("MyFirstFlow", "MyDataflow", null);
            fail();
        }
        catch (EPDataFlowAlreadyExistsException ex) {
            assertEquals("Data flow saved configuration by name 'MyFirstFlow' already exists", ex.getMessage());
        }

        // remove it
        assertTrue(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        assertFalse(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        assertEquals(0, dataFlowRuntime.getSavedConfigurations().length);
        assertNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));

        // add once more to instantiate
        dataFlowRuntime.saveConfiguration("MyFirstFlow", "MyDataflow", null);
        EPDataFlowInstance instance = dataFlowRuntime.instantiateSavedConfiguration("MyFirstFlow");
        epService.getEPAdministrator().createEPL("select * from MyEvent").addListener(listener);
        instance.run();
        assertTrue(listener.getAndClearIsInvoked());
        EPAssertionUtil.assertEqualsExactOrder(new String[] {"MyFirstFlow"}, dataFlowRuntime.getSavedConfigurations());
        assertNotNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));

        // add/remove instance
        dataFlowRuntime.saveInstance("F1", instance);
        EPAssertionUtil.assertEqualsExactOrder(new String[] {"F1"}, dataFlowRuntime.getSavedInstances());
        EPDataFlowInstance instanceFromSvc = dataFlowRuntime.getSavedInstance("F1");
        assertEquals("MyDataflow", instanceFromSvc.getDataFlowName());
        try {
            dataFlowRuntime.saveInstance("F1", instance);
            fail();
        }
        catch (EPDataFlowAlreadyExistsException ex) {
            // expected
            assertEquals("Data flow instance name 'F1' already saved", ex.getMessage());
        }
        assertTrue(dataFlowRuntime.removeSavedInstance("F1"));
        assertFalse(dataFlowRuntime.removeSavedInstance("F1"));
    }
}
