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

public class MultiMatchHandlerNoSubqueryNoDedup implements MultiMatchHandler {

    protected static final MultiMatchHandlerNoSubqueryNoDedup INSTANCE = new MultiMatchHandlerNoSubqueryNoDedup();

    private MultiMatchHandlerNoSubqueryNoDedup() {
    }

    public void handle(Collection<FilterHandleCallback> callbacks, EventBean theEvent) {
        for (FilterHandleCallback callback : callbacks) {
            callback.matchFound(theEvent, callbacks);
        }
    }
}
