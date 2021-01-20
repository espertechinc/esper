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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import static org.junit.Assert.assertEquals;

public class EPLInsertIntoIRStreamFunc implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        String[] fields = "c0,c1".split(",");
        RegressionPath path = new RegressionPath();

        String stmtTextOne = "@name('i0') insert irstream into MyStream " +
            "select irstream theString as c0, istream() as c1 " +
            "from SupportBean#lastevent";
        env.compileDeploy(stmtTextOne, path).addListener("i0");

        String stmtTextTwo = "@name('s0') select * from MyStream";
        env.compileDeploy(stmtTextTwo, path).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 0));
        env.assertPropsNew("i0", fields, new Object[]{"E1", true});
        env.assertPropsNew("s0", fields, new Object[]{"E1", true});

        env.sendEventBean(new SupportBean("E2", 0));
        env.assertPropsIRPair("i0", fields, new Object[]{"E2", true}, new Object[]{"E1", false});
        env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"E2", true}, {"E1", false}}, new Object[0][]);

        env.sendEventBean(new SupportBean("E3", 0));
        env.assertPropsIRPair("i0", fields, new Object[]{"E3", true}, new Object[]{"E2", false});
        env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"E3", true}, {"E2", false}}, new Object[0][]);

        // test SODA
        String eplModel = "@name('s1') select istream() from SupportBean";
        env.eplToModelCompileDeploy(eplModel);
        env.assertStatement("s1", statement -> assertEquals(Boolean.class, statement.getEventType().getPropertyType("istream()")));

        // test join
        env.undeployAll();
        fields = "c0,c1,c2".split(",");
        String stmtTextJoin = "@name('s0') select irstream theString as c0, id as c1, istream() as c2 " +
            "from SupportBean#lastevent, SupportBean_S0#lastevent";
        env.compileDeploy(stmtTextJoin).addListener("s0");
        env.sendEventBean(new SupportBean("E1", 0));
        env.sendEventBean(new SupportBean_S0(10));
        env.assertPropsNew("s0", fields, new Object[]{"E1", 10, true});

        env.sendEventBean(new SupportBean("E2", 0));
        env.assertPropsIRPair("s0", fields, new Object[]{"E2", 10, true}, new Object[]{"E1", 10, false});

        env.undeployAll();
    }
}
