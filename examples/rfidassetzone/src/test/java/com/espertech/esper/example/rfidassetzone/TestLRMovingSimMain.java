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
package com.espertech.esper.example.rfidassetzone;

import junit.framework.TestCase;

public class TestLRMovingSimMain extends TestCase {
    public void testSim() throws Exception {
        LRMovingSimMain main = new LRMovingSimMain(1, 100, 5, true, "LRMovingMainSample", false);
        main.run();
    }
}
