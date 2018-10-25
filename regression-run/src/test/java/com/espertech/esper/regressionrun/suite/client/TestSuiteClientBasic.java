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

import com.espertech.esper.regressionlib.suite.client.basic.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteClientBasic extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType("SupportBean", SupportBean.class);
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientBasicSelect() {
        RegressionRunner.run(session, new ClientBasicSelect());
    }

    public void testClientBasicFilter() {
        RegressionRunner.run(session, new ClientBasicFilter());
    }

    public void testClientBasicSelectClause() {
        RegressionRunner.run(session, new ClientBasicSelectClause());
    }

    public void testClientBasicAggregation() {
        RegressionRunner.run(session, new ClientBasicAggregation());
    }

    public void testClientBasicLengthWindow() {
        RegressionRunner.run(session, new ClientBasicLengthWindow());
    }

    public void testClientBasicPattern() {
        RegressionRunner.run(session, new ClientBasicPattern());
    }

    public void testClientBasicAnnotation() {
        RegressionRunner.run(session, new ClientBasicAnnotation());
    }
}
