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
package com.espertech.esper.regressionlib.suite.event.objectarray;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.Arrays;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EventObjectArrayInheritanceConfigInit implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runObjectArrInheritanceAssertion(env, new RegressionPath());
    }

    protected static void runObjectArrInheritanceAssertion(RegressionEnvironment env, RegressionPath path) {
        SupportUpdateListener[] listeners = new SupportUpdateListener[5];
        String[] statements = {
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
            "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
            "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++) {
            env.compileDeploy("@name('s" + i + "') " + statements[i], path);
            listeners[i] = new SupportUpdateListener();
            env.statement("s" + i).addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured("SubAEvent");
        assertEquals("base", type.getPropertyDescriptors()[0].getPropertyName());
        assertEquals("sub1", type.getPropertyDescriptors()[1].getPropertyName());
        assertEquals("suba", type.getPropertyDescriptors()[2].getPropertyName());
        assertEquals(3, type.getPropertyDescriptors().length);

        type = env.runtime().getEventTypeService().getEventTypePreconfigured("SubBEvent");
        assertEquals("[base, sub1, suba, subb]", Arrays.toString(type.getPropertyNames()));
        assertEquals(4, type.getPropertyDescriptors().length);

        type = env.runtime().getEventTypeService().getEventTypePreconfigured("Sub1Event");
        assertEquals("[base, sub1]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        type = env.runtime().getEventTypeService().getEventTypePreconfigured("Sub2Event");
        assertEquals("[base, sub2]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        env.sendEventObjectArray(new Object[]{"a", "b", "x"}, "SubAEvent");    // base, sub1, suba
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});

        env.sendEventObjectArray(new Object[]{"f1", "f2", "f4"}, "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});

        env.sendEventObjectArray(new Object[]{"XBASE", "X1", "X2", "XY"}, "SubBEvent");
        Object[] values = new Object[]{"XBASE", "X1", null, "X2", "XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        env.sendEventObjectArray(new Object[]{"YBASE", "Y1"}, "Sub1Event");
        values = new Object[]{"YBASE", "Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        env.sendEventObjectArray(new Object[]{"YBASE", "Y2"}, "Sub2Event");
        values = new Object[]{"YBASE", null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        env.sendEventObjectArray(new Object[]{"ZBASE"}, "RootEvent");
        values = new Object[]{"ZBASE", null, null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        // try property not available
        tryInvalidCompile(env, path, "select suba from Sub1Event", "Failed to validate select-clause expression 'suba': Property named 'suba' is not valid in any stream (did you mean 'sub1'?)");

        env.undeployAll();
    }
}
