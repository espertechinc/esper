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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.suite.event.json.*;
import com.espertech.esper.regressionlib.support.json.SupportJsonFieldAdapterStringDate;
import com.espertech.esper.regressionlib.support.json.SupportJsonFieldAdapterStringPoint;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEventJson extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventJsonVisibility() {
        RegressionRunner.run(session, EventJsonVisibility.executions());
    }

    public void testEventJsonTypingCoreParse() {
        RegressionRunner.run(session, EventJsonTypingCoreParse.executions());
    }

    public void testEventJsonTypingCoreWrite() {
        RegressionRunner.run(session, EventJsonTypingCoreWrite.executions());
    }

    public void testEventJsonUnderlying() {
        RegressionRunner.run(session, EventJsonUnderlying.executions());
    }

    public void testEventJsonInherits() {
        RegressionRunner.run(session, EventJsonInherits.executions());
    }

    public void testEventJsonEventSender() {
        RegressionRunner.run(session, EventJsonEventSender.executions());
    }

    public void testEventJsonParserLaxness() {
        RegressionRunner.run(session, EventJsonParserLaxness.executions());
    }

    public void testEventJsonCreateSchema() {
        RegressionRunner.run(session, EventJsonCreateSchema.executions());
    }

    public void testEventJsonGetter() {
        RegressionRunner.run(session, EventJsonGetter.executions());
    }

    public void testEventJsonDocSamples() {
        RegressionRunner.run(session, EventJsonDocSamples.executions());
    }

    public void testEventJsonAdapter() {
        RegressionRunner.run(session, EventJsonAdapter.executions());
    }

    public void testEventJsonProvidedUnderlyingClass() {
        RegressionRunner.run(session, EventJsonProvidedUnderlyingClass.executions());
    }

    public void testEventJsonTypingClassParseWrite() {
        RegressionRunner.run(session, EventJsonTypingClassParseWrite.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        for (Class clazz : new Class[] {SupportEnum.class, SupportJsonFieldAdapterStringDate.class, SupportJsonFieldAdapterStringPoint.class}) {
            configuration.getCommon().getImports().add(clazz.getName());
        }
    }
}
