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
package com.espertech.esper.example.autoid;

import junit.framework.TestCase;

public class TestAutoIdSimMain extends TestCase {
    public void testRun() throws Exception {
        AutoIdSimMain main = new AutoIdSimMain(10, "AutoIdSample", false);
        main.run();
    }
}
