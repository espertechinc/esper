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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.suite.client.multitenancy.ClientMultitenancyInsertInto;
import com.espertech.esper.regressionlib.suite.client.multitenancy.ClientMultitenancyProtected;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteClientMultitenancy extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientMultitenancyProtected() {
        RegressionRunner.run(session, ClientMultitenancyProtected.executions());
    }

    public void testClientMultitenancyInsertInto() {
        RegressionRunner.run(session, ClientMultitenancyInsertInto.executions());
    }

    private static void configure(Configuration configuration) {
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
    }
}
