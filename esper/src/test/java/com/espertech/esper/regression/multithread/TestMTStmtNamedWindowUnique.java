/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class TestMTStmtNamedWindowUnique extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestMTStmtNamedWindowUnique.class);
    private EPServiceProvider engine;

    public void tearDown()
    {
        engine.initialize();
    }

    public void testOrderedDeliverySpin() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
    }
}
