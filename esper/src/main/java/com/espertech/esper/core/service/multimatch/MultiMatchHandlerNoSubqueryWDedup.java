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
package com.espertech.esper.core.service.multimatch;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filter.FilterHandleCallback;

import java.util.Collection;
import java.util.LinkedHashSet;

public class MultiMatchHandlerNoSubqueryWDedup implements MultiMatchHandler {
    protected static final MultiMatchHandlerNoSubqueryWDedup INSTANCE = new MultiMatchHandlerNoSubqueryWDedup();

    private MultiMatchHandlerNoSubqueryWDedup() {
    }

    protected final static ThreadLocal<LinkedHashSet<FilterHandleCallback>> DEDUPS = new ThreadLocal<LinkedHashSet<FilterHandleCallback>>() {
        protected synchronized LinkedHashSet<FilterHandleCallback> initialValue() {
            return new LinkedHashSet<FilterHandleCallback>();
        }
    };

    public void handle(Collection<FilterHandleCallback> callbacks, EventBean theEvent) {

        if (callbacks.size() >= 8) {
            LinkedHashSet<FilterHandleCallback> dedup = DEDUPS.get();
            dedup.clear();
            dedup.addAll(callbacks);
            for (FilterHandleCallback callback : dedup) {
                callback.matchFound(theEvent, callbacks);
            }
            dedup.clear();
        } else {
            int count = 0;
            for (FilterHandleCallback callback : callbacks) {
                boolean haveInvoked = checkDup(callback, callbacks, count);
                if (!haveInvoked) {
                    callback.matchFound(theEvent, callbacks);
                }
                count++;
            }
        }
    }

    private boolean checkDup(FilterHandleCallback callback, Collection<FilterHandleCallback> callbacks, int count) {
        if (count < 1) {
            return false;
        }

        int index = 0;
        for (FilterHandleCallback candidate : callbacks) {
            if (candidate == callback) {
                return true;
            }

            index++;
            if (index == count) {
                break;
            }
        }

        return false;
    }
}
