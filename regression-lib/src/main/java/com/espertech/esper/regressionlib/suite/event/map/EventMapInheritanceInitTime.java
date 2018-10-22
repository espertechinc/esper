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
package com.espertech.esper.regressionlib.suite.event.map;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class EventMapInheritanceInitTime implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("base", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("sub1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("suba", String.class, null, false, false, false, false, false),
        }, env.runtime().getEventTypeService().getEventTypePreconfigured("SubAEvent").getPropertyDescriptors());

        runAssertionMapInheritance(env, new RegressionPath());
    }

    protected static void runAssertionMapInheritance(RegressionEnvironment env, RegressionPath path) {
        SupportUpdateListener[] listeners = new SupportUpdateListener[5];
        String[] statements = {
            "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
            "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
            "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
            "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++) {
            env.compileDeploy("@name('s" + i + "') " + statements[i], path);
            listeners[i] = new SupportUpdateListener();
            env.statement("s" + i).addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        env.sendEventMap(EventMapCore.makeMap("base=a,sub1=b,sub2=x,suba=c,subb=y"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});

        env.sendEventMap(EventMapCore.makeMap("base=f1,sub1=f2,sub2=f3,suba=f4,subb=f5"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});

        env.sendEventMap(EventMapCore.makeMap("base=XBASE,sub1=X1,sub2=X2,subb=XY"), "SubBEvent");
        Object[] values = new Object[]{"XBASE", "X1", "X2", null, "XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[3].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        env.sendEventMap(EventMapCore.makeMap("base=YBASE,sub1=Y1"), "Sub1Event");
        values = new Object[]{"YBASE", "Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        env.sendEventMap(EventMapCore.makeMap("base=YBASE,sub2=Y2"), "Sub2Event");
        values = new Object[]{"YBASE", null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        env.sendEventMap(EventMapCore.makeMap("base=ZBASE"), "RootEvent");
        values = new Object[]{"ZBASE", null, null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        // try property not available
        tryInvalidCompile(env, path, "select suba from Sub1Event",
            "Failed to validate select-clause expression 'suba': Property named 'suba' is not valid in any stream (did you mean 'sub1'?) [select suba from Sub1Event]");

        env.undeployAll();
    }
}
