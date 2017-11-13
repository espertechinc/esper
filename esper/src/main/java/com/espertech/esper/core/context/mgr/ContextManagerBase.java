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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.context.ContextPartitionStateListener;
import com.espertech.esper.core.service.EPServicesContext;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ContextManagerBase {
    protected final String contextName;
    protected final EPServicesContext servicesContext;
    protected CopyOnWriteArrayList<ContextPartitionStateListener> listenersLazy;

    public ContextManagerBase(String contextName, EPServicesContext servicesContext) {
        this.contextName = contextName;
        this.servicesContext = servicesContext;
    }

    public synchronized void addListener(ContextPartitionStateListener listener) {
        if (listenersLazy == null) {
            listenersLazy = new CopyOnWriteArrayList<>();
        }
        listenersLazy.add(listener);
    }

    public void removeListener(ContextPartitionStateListener listener) {
        if (listenersLazy == null) {
            return;
        }
        listenersLazy.remove(listener);
    }

    public Iterator<ContextPartitionStateListener> getListeners() {
        return listenersLazy == null ? Collections.emptyIterator() : Collections.unmodifiableCollection(listenersLazy).iterator();
    }

    public void removeListeners() {
        if (listenersLazy == null) {
            return;
        }
        listenersLazy.clear();
    }
}
