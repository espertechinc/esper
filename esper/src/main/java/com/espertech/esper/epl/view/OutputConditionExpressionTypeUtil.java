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
package com.espertech.esper.epl.view;

import java.util.LinkedHashMap;

public class OutputConditionExpressionTypeUtil {
    public final static LinkedHashMap<String, Object> TYPEINFO;

    static {
        TYPEINFO = new LinkedHashMap<String, Object>();
        TYPEINFO.put("count_insert", Integer.class);
        TYPEINFO.put("count_remove", Integer.class);
        TYPEINFO.put("count_insert_total", Integer.class);
        TYPEINFO.put("count_remove_total", Integer.class);
        TYPEINFO.put("last_output_timestamp", Long.class);
    }

    public static Object[] getOAPrototype() {
        return new Object[TYPEINFO.size()];
    }

    public static void populate(Object[] builtinProperties, int totalNewEventsCount, int totalOldEventsCount,
                                int totalNewEventsSum, int totalOldEventsSum, Long lastOutputTimestamp) {
        builtinProperties[0] = totalNewEventsCount;
        builtinProperties[1] = totalOldEventsCount;
        builtinProperties[2] = totalNewEventsSum;
        builtinProperties[3] = totalOldEventsSum;
        builtinProperties[4] = lastOutputTimestamp;
    }
}
