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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.epl.insertinto.EPLInsertIntoEventPrecedence;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLInsertIntoWConfig extends TestCase {

    public void testEPLInsertIntoPrecedenceLatched() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getRuntime().getExecution().setPrecedenceEnabled(true);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().addEventType(SupportBeanNumeric.class);
        RegressionRunner.run(session, EPLInsertIntoEventPrecedence.executions());
        session.destroy();
    }

    public void testEPLInsertIntoPrecedenceNoLatch() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getRuntime().getExecution().setPrecedenceEnabled(true);
        session.getConfiguration().getRuntime().getThreading().setInsertIntoDispatchPreserveOrder(false);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, EPLInsertIntoEventPrecedence.executionsNoLatch());
        session.destroy();
    }
}
