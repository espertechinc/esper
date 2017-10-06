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
package com.espertech.esperio.db.core;

import com.espertech.esper.client.EventBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnableDMLFactory implements RunnableFactory {
    private final static Logger log = LoggerFactory.getLogger(RunnableDMLFactory.class);

    private final RunnableDMLContext context;

    public RunnableDMLFactory(RunnableDMLContext context) {
        this.context = context;
    }

    public RunnableDMLContext getContext() {
        return context;
    }

    public Runnable makeRunnable(EventBean theEvent) {
        return new RunnableDML(context, theEvent);
    }
}
