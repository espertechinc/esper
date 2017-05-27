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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;

import static com.espertech.esper.regression.event.map.ExecEventMap.makeMap;
import static org.junit.Assert.*;

public class ExecEventObjectArrayInheritanceConfigInit implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("RootEvent", new String[]{"base"}, new Object[]{String.class});
        configuration.addEventType("Sub1Event", new String[]{"sub1"}, new Object[]{String.class});
        configuration.addEventType("Sub2Event", new String[]{"sub2"}, new Object[]{String.class});
        configuration.addEventType("SubAEvent", new String[]{"suba"}, new Object[]{String.class});
        configuration.addEventType("SubBEvent", new String[]{"subb"}, new Object[]{String.class});

        configuration.addObjectArraySuperType("Sub1Event", "RootEvent");
        configuration.addObjectArraySuperType("Sub2Event", "RootEvent");
        configuration.addObjectArraySuperType("SubAEvent", "Sub1Event");
        configuration.addObjectArraySuperType("SubBEvent", "SubAEvent");

        try {
            configuration.addObjectArraySuperType("SubBEvent", "Sub2Event");
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Object-array event types may not have multiple supertypes", ex.getMessage());
        }
    }

    public void run(EPServiceProvider epService) throws Exception {
        runObjectArrInheritanceAssertion(epService);
    }

    protected static void runObjectArrInheritanceAssertion(EPServiceProvider epService) {
        SupportUpdateListener[] listeners = new SupportUpdateListener[5];
        String[] statements = {
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
            "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
            "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++) {
            EPStatement statement = epService.getEPAdministrator().createEPL(statements[i]);
            listeners[i] = new SupportUpdateListener();
            statement.addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        EventType type = epService.getEPAdministrator().getConfiguration().getEventType("SubAEvent");
        assertEquals("base", type.getPropertyDescriptors()[0].getPropertyName());
        assertEquals("sub1", type.getPropertyDescriptors()[1].getPropertyName());
        assertEquals("suba", type.getPropertyDescriptors()[2].getPropertyName());
        assertEquals(3, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("SubBEvent");
        assertEquals("[base, sub1, suba, subb]", Arrays.toString(type.getPropertyNames()));
        assertEquals(4, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("Sub1Event");
        assertEquals("[base, sub1]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("Sub2Event");
        assertEquals("[base, sub2]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        epService.getEPRuntime().sendEvent(new Object[]{"a", "b", "x"}, "SubAEvent");    // base, sub1, suba
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});

        epService.getEPRuntime().sendEvent(new Object[]{"f1", "f2", "f4"}, "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});

        epService.getEPRuntime().sendEvent(new Object[]{"XBASE", "X1", "X2", "XY"}, "SubBEvent");
        Object[] values = new Object[]{"XBASE", "X1", null, "X2", "XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[]{"YBASE", "Y1"}, "Sub1Event");
        values = new Object[]{"YBASE", "Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[]{"YBASE", "Y2"}, "Sub2Event");
        values = new Object[]{"YBASE", null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[]{"ZBASE"}, "RootEvent");
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
            epService.getEPAdministrator().getConfiguration().addEventType("Sub1Event", makeMap(""), new String[]{"doodle"});
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Supertype by name 'doodle' could not be found", ex.getMessage());
        }
    }
}
