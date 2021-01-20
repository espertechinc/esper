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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithThis;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EPLInsertIntoTransposePattern {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoThisAsColumn());
        execs.add(new EPLInsertIntoTransposePOJOEventPattern());
        execs.add(new EPLInsertIntoTransposeMapEventPattern());
        return execs;
    }

    private static class EPLInsertIntoThisAsColumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('window') @public create window OneWindow#time(1 day) as select theString as alertId, this from SupportBeanWithThis", path);
            env.compileDeploy("insert into OneWindow select '1' as alertId, stream0.quote.this as this " +
                " from pattern [every quote=SupportBeanWithThis(theString='A')] as stream0", path);
            env.compileDeploy("insert into OneWindow select '2' as alertId, stream0.quote as this " +
                " from pattern [every quote=SupportBeanWithThis(theString='B')] as stream0", path);

            env.sendEventBean(new SupportBeanWithThis("A", 10));
            env.assertPropsPerRowIteratorAnyOrder("window", new String[]{"alertId", "this.intPrimitive"}, new Object[][]{{"1", 10}});

            env.sendEventBean(new SupportBeanWithThis("B", 20));
            env.assertPropsPerRowIteratorAnyOrder("window", new String[]{"alertId", "this.intPrimitive"}, new Object[][]{{"1", 10}, {"2", 20}});

            env.compileDeploy("@Name('window-2') @public create window TwoWindow#time(1 day) as select theString as alertId, * from SupportBeanWithThis", path);
            env.compileDeploy("insert into TwoWindow select '3' as alertId, quote.* " +
                " from pattern [every quote=SupportBeanWithThis(theString='C')] as stream0", path);

            env.sendEventBean(new SupportBeanWithThis("C", 30));
            env.assertPropsPerRowIteratorAnyOrder("window-2", new String[]{"alertId", "intPrimitive"}, new Object[][]{{"3", 30}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposePOJOEventPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "@public insert into MyStreamABBean select a, b from pattern [a=SupportBean_A -> b=SupportBean_B]";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s0') select a.id, b.id from MyStreamABBean";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            env.assertPropsNew("s0", "a.id,b.id".split(","), new Object[]{"A1", "B1"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeMapEventPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String stmtTextOne = "@name('i1') @public insert into MyStreamABMap select a, b from pattern [a=AEventMap -> b=BEventMap]";
            env.compileDeploy(stmtTextOne, path).addListener("i1");
            env.assertStatement("i1", statement -> {
                assertEquals(Map.class, statement.getEventType().getPropertyType("a"));
                assertEquals(Map.class, statement.getEventType().getPropertyType("b"));
            });

            String stmtTextTwo = "@name('s0') select a.id, b.id from MyStreamABMap";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");
            env.assertStatement("s0", statement -> {
                assertEquals(String.class, statement.getEventType().getPropertyType("a.id"));
                assertEquals(String.class, statement.getEventType().getPropertyType("b.id"));
            });

            Map<String, Object> eventOne = makeMap(new Object[][]{{"id", "A1"}});
            Map<String, Object> eventTwo = makeMap(new Object[][]{{"id", "B1"}});

            env.sendEventMap(eventOne, "AEventMap");
            env.sendEventMap(eventTwo, "BEventMap");

            env.assertPropsNew("s0", "a.id,b.id".split(","), new Object[]{"A1", "B1"});
            env.assertPropsNew("i1", "a,b".split(","), new Object[]{eventOne, eventTwo});

            env.undeployAll();
        }
    }

    private static Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (Object[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }
}
