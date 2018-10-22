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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class InfraNWTableSubqCorrelCoerce {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        // named window tests
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, false, false, false)); // no share
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, false, false, true)); // no share create index
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, true, false, false)); // share
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, true, false, true)); // share create index
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, true, true, false)); // disable share
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(true, true, true, true)); // disable share create index

        // table tests
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(false, false, false, false)); // table
        execs.add(new InfraNWTableSubqCorrelCoerceSimple(false, false, false, true)); // table + create index
        return execs;
    }

    private static class InfraNWTableSubqCorrelCoerceSimple implements RegressionExecution {
        private final boolean namedWindow;
        private final boolean enableIndexShareCreate;
        private final boolean disableIndexShareConsumer;
        private final boolean createExplicitIndex;

        public InfraNWTableSubqCorrelCoerceSimple(boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex) {
            this.namedWindow = namedWindow;
            this.enableIndexShareCreate = enableIndexShareCreate;
            this.disableIndexShareConsumer = disableIndexShareConsumer;
            this.createExplicitIndex = createExplicitIndex;
        }

        public void run(RegressionEnvironment env) {
            EPCompiled c1 = env.compileWBusPublicType("create schema EventSchema(e0 string, e1 int, e2 string)");
            EPCompiled c2 = env.compileWBusPublicType("create schema WindowSchema(col0 string, col1 long, col2 string)");
            RegressionPath path = new RegressionPath();
            path.add(c1);
            path.add(c2);
            env.deploy(c1);
            env.deploy(c2);

            String createEpl = namedWindow ?
                "create window MyInfra#keepall as WindowSchema" :
                "create table MyInfra (col0 string primary key, col1 long, col2 string)";
            if (enableIndexShareCreate) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);
            env.compileDeploy("insert into MyInfra select * from WindowSchema", path);

            if (createExplicitIndex) {
                env.compileDeploy("@name('index') create index MyIndex on MyInfra (col2, col1)", path);
            }

            String[] fields = "e0,val".split(",");
            String consumeEpl = "@name('s0') select e0, (select col0 from MyInfra where col2 = es.e2 and col1 = es.e1) as val from EventSchema es";
            if (disableIndexShareConsumer) {
                consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
            }
            env.compileDeploy(consumeEpl, path).addListener("s0");

            sendWindow(env, "W1", 10L, "c31");
            sendEvent(env, "E1", 10, "c31");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "W1"});

            sendEvent(env, "E2", 11, "c32");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", null});

            sendWindow(env, "W2", 11L, "c32");
            sendEvent(env, "E3", 11, "c32");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", "W2"});

            sendWindow(env, "W3", 11L, "c31");
            sendWindow(env, "W4", 10L, "c32");

            sendEvent(env, "E4", 11, "c31");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "W3"});

            sendEvent(env, "E5", 10, "c31");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", "W1"});

            sendEvent(env, "E6", 10, "c32");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

            // test late start
            env.undeployModuleContaining("s0");
            env.compileDeploy(consumeEpl, path).addListener("s0");

            sendEvent(env, "E6", 10, "c32");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

            env.undeployModuleContaining("s0");
            if (env.statement("index") != null) {
                env.undeployModuleContaining("index");
            }

            env.undeployAll();
        }
    }

    private static void sendWindow(RegressionEnvironment env, String col0, long col1, String col2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("col0", col0);
        theEvent.put("col1", col1);
        theEvent.put("col2", col2);
        if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
            env.sendEventObjectArray(theEvent.values().toArray(), "WindowSchema");
        } else {
            env.sendEventMap(theEvent, "WindowSchema");
        }
    }

    private static void sendEvent(RegressionEnvironment env, String e0, int e1, String e2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("e0", e0);
        theEvent.put("e1", e1);
        theEvent.put("e2", e2);
        if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
            env.sendEventObjectArray(theEvent.values().toArray(), "EventSchema");
        } else {
            env.sendEventMap(theEvent, "EventSchema");
        }
    }
}
