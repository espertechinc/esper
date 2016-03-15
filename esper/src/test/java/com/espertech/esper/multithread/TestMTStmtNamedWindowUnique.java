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

package com.espertech.esper.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMTUpdateListener;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class TestMTStmtNamedWindowUnique extends TestCase
{
    private static final Log log = LogFactory.getLog(TestMTStmtNamedWindowUnique.class);
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
