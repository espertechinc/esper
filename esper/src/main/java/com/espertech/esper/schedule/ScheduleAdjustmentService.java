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
package com.espertech.esper.schedule;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for holding expiration dates to adjust.
 */
public class ScheduleAdjustmentService {
    private Set<ScheduleAdjustmentCallback> callbacks;

    /**
     * Add a callback
     *
     * @param callback to add
     */
    public synchronized void addCallback(ScheduleAdjustmentCallback callback) {
        if (callbacks == null) {
            callbacks = new HashSet<ScheduleAdjustmentCallback>();
        }
        callbacks.add(callback);
    }

    /**
     * Make callbacks to adjust expiration dates.
     *
     * @param delta to adjust for
     */
    public void adjust(long delta) {
        if (callbacks == null) {
            return;
        }
        for (ScheduleAdjustmentCallback callback : callbacks) {
            callback.adjust(delta);
        }
    }

    public void removeCallback(ScheduleAdjustmentCallback callback) {
        if (callbacks == null) {
            return;
        }
        callbacks.remove(callback);
    }
}
