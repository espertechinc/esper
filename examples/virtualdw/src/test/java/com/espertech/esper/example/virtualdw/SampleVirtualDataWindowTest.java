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
package com.espertech.esper.example.virtualdw;

import junit.framework.TestCase;

/**
 * Test case for Large-External Data Window.
 */
public class SampleVirtualDataWindowTest extends TestCase {
    public void testSample() {
        SampleVirtualDataWindowMain main = new SampleVirtualDataWindowMain();
        main.run();
    }
}
