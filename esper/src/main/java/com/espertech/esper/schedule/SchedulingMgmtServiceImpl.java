/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.schedule;

import com.espertech.esper.timer.TimeSourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Implements the schedule service by simply keeping a sorted set of long millisecond
 * values and a set of handles for each.
 * <p>
 * Synchronized since statement creation and event evaluation by multiple (event send) threads
 * can lead to callbacks added/removed asynchronously.
 */
public final class SchedulingMgmtServiceImpl implements SchedulingMgmtService
{
    // Current bucket number - for use in ordering handles by bucket
    private int curBucketNum;

    /**
     * Constructor.
     */
    public SchedulingMgmtServiceImpl()
    {
    }

    public void destroy()
    {
        log.debug("Destroying scheduling management service");
    }

    public synchronized ScheduleBucket allocateBucket()
    {
        curBucketNum++;
        return new ScheduleBucket(curBucketNum);
    }

    private static final Log log = LogFactory.getLog(SchedulingMgmtServiceImpl.class);
}