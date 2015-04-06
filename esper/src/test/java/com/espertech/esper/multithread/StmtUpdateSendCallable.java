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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.support.bean.SupportBean;
import junit.framework.AssertionFailedError;

public class StmtUpdateSendCallable implements Callable
{
    private static final Log log = LogFactory.getLog(StmtUpdateSendCallable.class);
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int numRepeats;

    public StmtUpdateSendCallable(int threadNum, EPServiceProvider engine, int numRepeats)
    {
        this.threadNum = threadNum;
        this.engine = engine;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception
    {
        try
        {
            log.info(".call Thread " + Thread.currentThread().getId() + " sending " + numRepeats + " events");
            for (int loop = 0; loop < numRepeats; loop++)
            {
                String id = Long.toString(threadNum * 100000000 + loop);
                SupportBean bean = new SupportBean(id, 0);
                engine.getEPRuntime().sendEvent(bean);
            }
            log.info(".call Thread " + Thread.currentThread().getId() + " completed.");
        }
        catch (AssertionFailedError ex)
        {
            log.fatal("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        catch (Throwable t)
        {
            log.fatal("Error in thread " + Thread.currentThread().getId(), t);
            return false;
        }
        return true;
    }
}
