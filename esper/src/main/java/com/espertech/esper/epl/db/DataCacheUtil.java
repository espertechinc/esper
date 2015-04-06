/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.db;

import com.espertech.esper.client.ConfigurationCacheReferenceType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.apachecommons.ReferenceMap;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.ExtensionServicesContext;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.ScheduleSlot;
import com.espertech.esper.schedule.SchedulingService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class DataCacheUtil
{
    public static Object getLookupKey(Object[] lookupKeys)
    {
        Object key;
        if (lookupKeys.length == 0) {
            key = Object.class;
        }
        else if (lookupKeys.length > 1) {
            key = new MultiKeyUntyped(lookupKeys);
        }
        else {
            key = lookupKeys[0];
        }
        return key;
    }
}
