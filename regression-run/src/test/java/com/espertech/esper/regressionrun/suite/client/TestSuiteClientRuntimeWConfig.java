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
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.suite.client.runtime.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.MyAnnotationValueEnum;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

public class TestSuiteClientRuntimeWConfig extends TestCase {

    public void testClientRuntimeRuntimeStateChange() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        new ClientRuntimeRuntimeProvider.ClientRuntimeRuntimeStateChange().run(configuration);
    }

    public void testClientRuntimeRuntimeDestroy() {
        Configuration config = SupportConfigFactory.getConfiguration();
        new ClientRuntimeRuntimeProvider.ClientRuntimeRuntimeDestroy().run(config);
    }

    public void testClientRuntimeJMX() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        new ClientRuntimeJMX().run(configuration);
    }

    public void testClientRuntimeExHandlerGetContext() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        new ClientRuntimeExceptionHandler.ClientRuntimeExHandlerGetContext().run(configuration);
    }

    public void testClientRuntimeExceptionHandlerNoHandler() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        new ClientRuntimeExceptionHandler.ClientRuntimeExceptionHandlerNoHandler().run(configuration);
    }

    public void testClientRuntimeInvalidMicroseconds() {
        Configuration config = SupportConfigFactory.getConfiguration();
        new ClientRuntimeRuntimeProvider.ClientRuntimeMicrosecondInvalid().run(config);
    }

    public void testClientRuntimeAnnotationImportInvalid() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addAnnotationImport(SupportEnum.class);
        session.getConfiguration().getCommon().addAnnotationImport(MyAnnotationValueEnum.class);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, new ClientRuntimeStatementAnnotation.ClientRuntimeAnnotationImportInvalid());
        session.destroy();
    }

    public void testClientRuntimeThreadedConfigInbound() {
        RegressionRunner.runConfigurable(new ClientRuntimeThreadedConfigInbound());
    }

    public void testClientRuntimeThreadedConfigInboundFastShutdown() {
        RegressionRunner.runConfigurable(new ClientRuntimeThreadedConfigInboundFastShutdown());
    }

    public void testClientRuntimeThreadedConfigOutbound() {
        RegressionRunner.runConfigurable(new ClientRuntimeThreadedConfigOutbound());
    }

    public void testClientRuntimeThreadedConfigRoute() {
        RegressionRunner.runConfigurable(new ClientRuntimeThreadedConfigRoute());
    }

    public void testClientRuntimeThreadedConfigTimer() {
        RegressionRunner.runConfigurable(new ClientRuntimeThreadedConfigTimer());
    }

    public void testClientRuntimeClockTypeRuntime() {
        new ClientRuntimeTimeControlClockType().run(SupportConfigFactory.getConfiguration());
    }

    public void testClientSubscriberDisallowed() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, new ClientRuntimeSubscriberDisallowed());
        session.destroy();
    }
}
