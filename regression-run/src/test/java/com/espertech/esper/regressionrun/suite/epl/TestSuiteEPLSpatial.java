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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.epl.spatial.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLSpatial extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLSpatialMXCIFQuadTreeEventIndex() {
        RegressionRunner.run(session, EPLSpatialMXCIFQuadTreeEventIndex.executions());
    }

    public void testEPLSpatialMXCIFQuadTreeFilterIndex() {
        RegressionRunner.run(session, EPLSpatialMXCIFQuadTreeFilterIndex.executions());
    }

    public void testEPLSpatialMXCIFQuadTreeInvalid() {
        RegressionRunner.run(session, EPLSpatialMXCIFQuadTreeInvalid.executions());
    }

    public void testEPLSpatialPointRegionQuadTreeEventIndex() {
        RegressionRunner.run(session, EPLSpatialPointRegionQuadTreeEventIndex.executions());
    }

    public void testEPLSpatialPointRegionQuadTreeFilterIndex() {
        RegressionRunner.run(session, EPLSpatialPointRegionQuadTreeFilterIndex.executions());
    }

    public void testEPLSpatialPointRegionQuadTreeInvalid() {
        RegressionRunner.run(session, EPLSpatialPointRegionQuadTreeInvalid.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportSpatialAABB.class, SupportSpatialEventRectangle.class,
            SupportSpatialDualAABB.class, SupportEventRectangleWithOffset.class, SupportSpatialPoint.class,
            SupportSpatialDualPoint.class}) {
            configuration.getCommon().addEventType(clazz);
        }
        configuration.getCommon().getLogging().setEnableQueryPlan(true);
    }
}
