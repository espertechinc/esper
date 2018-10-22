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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCombinedProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class EPLJoinPropertyAccess {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinRegularJoin());
        execs.add(new EPLJoinOuterJoin());
        return execs;
    }

    private static class EPLJoinRegularJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportBeanCombinedProps combined = SupportBeanCombinedProps.makeDefaultBean();
            SupportBeanComplexProps complex = SupportBeanComplexProps.makeDefaultBean();
            assertEquals("0ma0", combined.getIndexed(0).getMapped("0ma").getValue());

            String epl = "@name('s0') select nested.nested, s1.indexed[0], nested.indexed[1] from " +
                "SupportBeanComplexProps#length(3) nested, " +
                "SupportBeanCombinedProps#length(3) s1" +
                " where mapped('keyOne') = indexed[2].mapped('2ma').value and" +
                " indexed[0].mapped('0ma').value = '0ma0'";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(combined);
            env.sendEventBean(complex);

            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertSame(complex.getNested(), theEvent.get("nested.nested"));
            assertSame(combined.getIndexed(0), theEvent.get("s1.indexed[0]"));
            assertEquals(complex.getIndexed(1), theEvent.get("nested.indexed[1]"));

            env.undeployAll();
        }
    }

    private static class EPLJoinOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from " +
                "SupportBeanComplexProps#length(3) s0" +
                " left outer join " +
                "SupportBeanCombinedProps#length(3) s1" +
                " on mapped('keyOne') = indexed[2].mapped('2ma').value";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanCombinedProps combined = SupportBeanCombinedProps.makeDefaultBean();
            env.sendEventBean(combined);
            SupportBeanComplexProps complex = SupportBeanComplexProps.makeDefaultBean();
            env.sendEventBean(complex);

            // double check that outer join criteria match
            assertEquals(complex.getMapped("keyOne"), combined.getIndexed(2).getMapped("2ma").getValue());

            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("simple", theEvent.get("s0.simpleProperty"));
            assertSame(complex, theEvent.get("s0"));
            assertSame(combined, theEvent.get("s1"));

            env.undeployAll();
        }
    }
}
