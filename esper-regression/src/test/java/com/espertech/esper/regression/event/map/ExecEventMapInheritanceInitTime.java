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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class ExecEventMapInheritanceInitTime implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> root = ExecEventMap.makeMap(new Object[][]{{"base", String.class}});
        Map<String, Object> sub1 = ExecEventMap.makeMap(new Object[][]{{"sub1", String.class}});
        Map<String, Object> sub2 = ExecEventMap.makeMap(new Object[][]{{"sub2", String.class}});
        Properties suba = ExecEventMap.makeProperties(new Object[][]{{"suba", String.class}});
        Map<String, Object> subb = ExecEventMap.makeMap(new Object[][]{{"subb", String.class}});
        configuration.addEventType("RootEvent", root);
        configuration.addEventType("Sub1Event", sub1);
        configuration.addEventType("Sub2Event", sub2);
        configuration.addEventType("SubAEvent", suba);
        configuration.addEventType("SubBEvent", subb);

        configuration.addMapSuperType("Sub1Event", "RootEvent");
        configuration.addMapSuperType("Sub2Event", "RootEvent");
        configuration.addMapSuperType("SubAEvent", "Sub1Event");
        configuration.addMapSuperType("SubBEvent", "Sub1Event");
        configuration.addMapSuperType("SubBEvent", "Sub2Event");
    }

    public void run(EPServiceProvider epService) throws Exception {

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("base", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("sub1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("suba", String.class, null, false, false, false, false, false),
        }, ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("SubAEvent").getPropertyDescriptors());

        runAssertionMapInheritance(epService);
    }

    protected static void runAssertionMapInheritance(EPServiceProvider epService) {
        SupportUpdateListener[] listeners = new SupportUpdateListener[5];
        String[] statements = {
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
            "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
            "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++) {
            EPStatement statement = epService.getEPAdministrator().createEPL(statements[i]);
            listeners[i] = new SupportUpdateListener();
            statement.addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=a,sub1=b,sub2=x,suba=c,subb=y"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=f1,sub1=f2,sub2=f3,suba=f4,subb=f5"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=XBASE,sub1=X1,sub2=X2,subb=XY"), "SubBEvent");
        Object[] values = new Object[]{"XBASE", "X1", "X2", null, "XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[3].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=YBASE,sub1=Y1"), "Sub1Event");
        values = new Object[]{"YBASE", "Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=YBASE,sub2=Y2"), "Sub2Event");
        values = new Object[]{"YBASE", null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(ExecEventMap.makeMap("base=ZBASE"), "RootEvent");
        values = new Object[]{"ZBASE", null, null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        // try property not available
        try {
            epService.getEPAdministrator().createEPL("select suba from Sub1Event");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'suba': Property named 'suba' is not valid in any stream (did you mean 'sub1'?) [select suba from Sub1Event]", ex.getMessage());
        }

        // try supertype not exists
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("Sub1Event", ExecEventMap.makeMap(""), new String[]{"doodle"});
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Supertype by name 'doodle' could not be found", ex.getMessage());
        }
    }
}
