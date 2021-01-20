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
package com.espertech.esper.regressionlib.suite.epl.contained;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanArrayCollMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EPLContainedEventArray {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLContainedEventDocSample());
        execs.add(new EPLContainedEventIntArray());
        execs.add(new EPLContainedStringArrayWithWhere());
        return execs;
    }

    private static class EPLContainedStringArrayWithWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema MyRow(id String);\n" +
                "@public @buseventtype create schema MyEvent(idsBefore string[], idsAfter string[]);\n" +
                "@name('s0') select id from MyEvent[select idsBefore, * from idsAfter@type(MyRow)] where id not in (idsBefore);\n";
            env.compileDeploy(epl).addListener("s0");

            assertSend(env, "A,B,C", "D,E", new Object[][] {{"D"}, {"E"}});
            assertSend(env, "A,C", "C,A", null);
            assertSend(env, "A", "B", new Object[][] {{"B"}});
            assertSend(env, "A,B", "F,B,A", new Object[][] {{"F"}});

            env.undeployAll();
        }

        private void assertSend(RegressionEnvironment env, String idsBeforeCSV, String idsAfterCSV, Object[][] expected) {
            Map<String, Object> data = CollectionUtil.buildMap("idsBefore", idsBeforeCSV.split(","), "idsAfter", idsAfterCSV.split(","));
            env.sendEventMap(data, "MyEvent");
            if (expected == null) {
                env.assertListenerNotInvoked("s0");
            } else {
                env.assertPropsPerRowLastNew("s0", "id".split(","), expected);
            }
        }
    }

    private static class EPLContainedEventDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(
                "@public create schema IdContainer(id int);" +
                    "@public create schema MyEvent(ids int[]);" +
                    "select * from MyEvent[ids@type(IdContainer)];", path);

            env.compileDeploy(
                    "@public create window MyWindow#keepall (id int);" +
                    "on MyEvent[ids@type(IdContainer)] as my_ids \n" +
                    "delete from MyWindow my_window \n" +
                    "where my_ids.id = my_window.id;", path);

            env.undeployAll();
        }
    }

    private static class EPLContainedEventIntArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "create objectarray schema DeleteId(id int);" +
                "@public create window MyWindow#keepall as SupportBean;" +
                "insert into MyWindow select * from SupportBean;" +
                "on SupportBeanArrayCollMap[intArr@type(DeleteId)] delete from MyWindow where intPrimitive = id";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            assertCount(env, path, 2);
            env.sendEventBean(new SupportBeanArrayCollMap(new int[]{1, 2}));
            assertCount(env, path, 0);

            env.undeployAll();
        }
    }

    private static void assertCount(RegressionEnvironment env, RegressionPath path, long i) {
        env.assertThat(() -> assertEquals(i, env.compileExecuteFAF("select count(*) as c0 from MyWindow", path).getArray()[0].get("c0")));
    }
}
