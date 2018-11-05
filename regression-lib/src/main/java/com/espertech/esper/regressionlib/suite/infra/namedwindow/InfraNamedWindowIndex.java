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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowIndex implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('window') create window MyWindowOne#unique(theString) as SupportBean;\n" +
            "insert into MyWindowOne select * from SupportBean;\n" +
            "@name('idx') create unique index I1 on MyWindowOne(theString);\n";
        env.compileDeploy(epl);
        assertEquals(StatementType.CREATE_INDEX, env.statement("idx").getProperty(StatementProperty.STATEMENTTYPE));
        assertEquals("I1", env.statement("idx").getProperty(StatementProperty.CREATEOBJECTNAME));

        env.sendEventBean(new SupportBean("E0", 1));
        env.sendEventBean(new SupportBean("E2", 2));
        env.sendEventBean(new SupportBean("E2", 3));
        env.sendEventBean(new SupportBean("E1", 4));
        env.sendEventBean(new SupportBean("E0", 5));

        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("window"), "theString,intPrimitive".split(","), new Object[][]{{"E0", 5}, {"E1", 4}, {"E2", 3}});

        env.undeployAll();
    }
}
