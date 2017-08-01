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
package com.espertech.esper.regression.spatial;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteSpatial extends TestCase {
    public void testExecSpatialMXCIFQuadTreeEventIndex() {
        RegressionRunner.run(new ExecSpatialMXCIFQuadTreeEventIndex());
    }

    public void testExecSpatialMXCIFQuadTreeFilterIndex() {
        RegressionRunner.run(new ExecSpatialMXCIFQuadTreeFilterIndex());
    }

    public void testExecSpatialMXCIFQuadTreeInvalid() {
        RegressionRunner.run(new ExecSpatialMXCIFQuadTreeInvalid());
    }

    public void testExecSpatialPointRegionQuadTreeEventIndex() {
        RegressionRunner.run(new ExecSpatialPointRegionQuadTreeEventIndex());
    }

    public void testExecSpatialPointRegionQuadTreeFilterIndex() {
        RegressionRunner.run(new ExecSpatialPointRegionQuadTreeFilterIndex());
    }

    public void testExecSpatialPointRegionQuadTreeInvalid() {
        RegressionRunner.run(new ExecSpatialPointRegionQuadTreeInvalid());
    }
}
