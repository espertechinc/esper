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
package com.espertech.esper.common.internal.epl.historical.datacache;

import com.espertech.esper.common.internal.epl.index.base.EventTable;

/**
 * Null implementation for a data cache that doesn't ever hit.
 */
public class HistoricalDataCacheNullImpl implements HistoricalDataCache {
    public EventTable[] getCached(Object methodParams) {
        return null;
    }

    public void put(Object methodParams, EventTable[] rows) {
    }

    public boolean isActive() {
        return false;
    }

    public void destroy() {
    }
}
