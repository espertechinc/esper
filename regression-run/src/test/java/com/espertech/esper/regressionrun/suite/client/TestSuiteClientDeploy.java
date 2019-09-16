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
import com.espertech.esper.regressionlib.suite.client.deploy.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteClientDeploy extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        Configuration configuration = session.getConfiguration();
        configure(configuration);
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientDeployUndeploy() {
        RegressionRunner.run(session, ClientDeployUndeploy.executions());
    }

    public void testClientDeployPreconditionDependency() {
        RegressionRunner.run(session, ClientDeployPreconditionDependency.executions());
    }

    public void testClientDeployPreconditionDuplicate() {
        RegressionRunner.run(session, ClientDeployPreconditionDuplicate.executions());
    }

    public void testClientDeployUserObject() {
        RegressionRunner.run(session, ClientDeployUserObject.executions());
    }

    public void testClientDeployStatementName() {
        RegressionRunner.run(session, ClientDeployStatementName.executions());
    }

    public void testClientDeployResult() {
        RegressionRunner.run(session, ClientDeployResult.executions());
    }

    public void testClientDeployRedefinition() {
        RegressionRunner.run(session, ClientDeployRedefinition.executions());
    }

    public void testClientDeployVersion() {
        RegressionRunner.run(session, ClientDeployVersion.executions());
    }

    public void testClientDeployClassLoaderOption() {
        RegressionRunner.run(session, ClientDeployClassLoaderOption.executions());
    }

    private void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class}) {
            configuration.getCommon().addEventType(clazz);
        }
    }
}
