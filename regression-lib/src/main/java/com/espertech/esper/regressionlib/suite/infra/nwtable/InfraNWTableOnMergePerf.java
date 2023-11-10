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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InfraNWTableOnMergePerf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraPerformance(true, EventRepresentationChoice.OBJECTARRAY));
        execs.add(new InfraPerformance(true, EventRepresentationChoice.MAP));
        execs.add(new InfraPerformance(true, EventRepresentationChoice.DEFAULT));
        execs.add(new InfraPerformance(false, EventRepresentationChoice.OBJECTARRAY));
        return execs;
    }

    private static class InfraPerformance implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.PERFORMANCE);
        }

        private final boolean namedWindow;
        private final EventRepresentationChoice outputType;

        public InfraPerformance(boolean namedWindow, EventRepresentationChoice outputType) {
            this.namedWindow = namedWindow;
            this.outputType = outputType;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                outputType.getAnnotationText() + "@name('create') @public create window MyWindow#keepall as (c1 string, c2 int)" :
                "@name('create') @public create table MyWindow(c1 string primary key, c2 int)";
            env.compileDeploy(eplCreate, path);
            env.assertStatement("create", statement -> assertTrue(outputType.matchesClass(statement.getEventType().getUnderlyingType())));

            // preload events
            env.compileDeploy("@name('insert') insert into MyWindow select theString as c1, intPrimitive as c2 from SupportBean", path);
            final int totalUpdated = 5000;
            for (int i = 0; i < totalUpdated; i++) {
                env.sendEventBean(new SupportBean("E" + i, 0));
            }
            env.undeployModuleContaining("insert");

            String epl = "@name('s0') on SupportBean sb merge MyWindow nw where nw.c1 = sb.theString " +
                "when matched then update set nw.c2=sb.intPrimitive";
            env.compileDeploy(epl, path);

            // prime
            for (int i = 0; i < 100; i++) {
                env.sendEventBean(new SupportBean("E" + i, 1));
            }
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < totalUpdated; i++) {
                env.sendEventBean(new SupportBean("E" + i, 1));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            // verify
            env.assertIterator("create", events -> {
                int count = 0;
                for (; events.hasNext(); ) {
                    EventBean next = events.next();
                    assertEquals(1, next.get("c2"));
                    count++;
                }
                assertEquals(totalUpdated, count);
            });
            try {
                if (!(delta < 500)) {
                    assertTrue("Delta=" + delta, delta < 500 * 1.2);
                } else {
                    assertTrue("Delta=" + delta, delta < 500);
                }
            } catch (Exception e) {
                throw new AssertionError("Loop takes too long!");
            }
            

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "namedWindow=" + namedWindow +
                ", outputType=" + outputType +
                '}';
        }
    }
}
