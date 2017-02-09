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

public class MultiMatchHandlerSubqueryWDedup implements MultiMatchHandler {

    private final boolean subselectPreeval;

    protected MultiMatchHandlerSubqueryWDedup(boolean subselectPreeval) {
        this.subselectPreeval = subselectPreeval;
    }

    public void handle(Collection<FilterHandleCallback> callbacks, EventBean theEvent) {

        LinkedHashSet<FilterHandleCallback> dedup = MultiMatchHandlerNoSubqueryWDedup.DEDUPS.get();
        dedup.clear();
        dedup.addAll(callbacks);

        if (subselectPreeval) {
            // sub-selects always go first
            for (FilterHandleCallback callback : dedup) {
                if (callback.isSubSelect()) {
                    callback.matchFound(theEvent, dedup);
                }
            }

            for (FilterHandleCallback callback : dedup) {
                if (!callback.isSubSelect()) {
                    callback.matchFound(theEvent, dedup);
                }
            }
        } else {
            // sub-selects always go last
            for (FilterHandleCallback callback : dedup) {
                if (!callback.isSubSelect()) {
                    callback.matchFound(theEvent, dedup);
                }
            }

            for (FilterHandleCallback callback : dedup) {
                if (callback.isSubSelect()) {
                    callback.matchFound(theEvent, dedup);
                }
            }
        }

        dedup.clear();
    }
}
