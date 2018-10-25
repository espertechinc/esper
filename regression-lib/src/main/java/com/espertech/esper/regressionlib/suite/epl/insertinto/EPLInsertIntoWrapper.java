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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanSimple;
import com.espertech.esper.regressionlib.support.bean.SupportEventContainsSupportBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EPLInsertIntoWrapper {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoWrapperBean());
        execs.add(new EPLInsertInto3StreamWrapper());
        return execs;
    }

    public static class EPLInsertIntoWrapperBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('i1') insert into WrappedBean select *, intPrimitive as p0 from SupportBean", path);
            env.addListener("i1");

            env.compileDeploy("@name('i2') insert into WrappedBean select sb from SupportEventContainsSupportBean sb", path);
            env.addListener("i2");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("i1").assertOneGetNewAndReset(), "theString,intPrimitive,p0".split(","), new Object[]{"E1", 1, 1});

            env.sendEventBean(new SupportEventContainsSupportBean(new SupportBean("E2", 2)));
            EPAssertionUtil.assertProps(env.listener("i2").assertOneGetNewAndReset(), "theString,intPrimitive,p0".split(","), new Object[]{"E2", 2, null});

            env.undeployAll();
        }
    }

    public static class EPLInsertInto3StreamWrapper implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementOne = "@name('s0') insert into StreamA select irstream * from SupportBeanSimple#length(2)";
            String statementTwo = "@name('s1') insert into StreamB select irstream *, myString||'A' as propA from StreamA#length(2)";
            String statementThree = "@name('s2') insert into StreamC select irstream *, propA||'B' as propB from StreamB#length(2)";

            RegressionPath path = new RegressionPath();
            env.compileDeploy(statementOne, path);
            env.compileDeploy(statementTwo, path);
            env.compileDeploy(statementThree, path).addListener("s2");

            env.milestone(0);

            env.sendEventBean(new SupportBeanSimple("e1", 1));
            EventBean event = env.listener("s2").assertOneGetNewAndReset();
            assertEquals("e1", event.get("myString"));
            assertEquals("e1AB", event.get("propB"));

            env.milestone(1);

            env.sendEventBean(new SupportBeanSimple("e2", 1));
            event = env.listener("s2").assertOneGetNewAndReset();
            assertEquals("e2", event.get("myString"));
            assertEquals("e2AB", event.get("propB"));

            env.sendEventBean(new SupportBeanSimple("e3", 1));
            event = env.listener("s2").getLastNewData()[0];
            assertEquals("e3", event.get("myString"));
            assertEquals("e3AB", event.get("propB"));
            event = env.listener("s2").getLastOldData()[0];
            assertEquals("e1", event.get("myString"));
            assertEquals("e1AB", event.get("propB"));

            env.undeployAll();
        }
    }

}
