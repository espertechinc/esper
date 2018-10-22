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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;

/**
 * An instance of this class is associated with a specific named window. The processor
 * provides the views to create-window, on-delete statements and statements selecting from a named window.
 */
public class NamedWindowInstance {
    private final NamedWindowRootViewInstance rootViewInstance;
    private final NamedWindowTailViewInstance tailViewInstance;

    public NamedWindowInstance(NamedWindow processor, AgentInstanceContext agentInstanceContext) {
        rootViewInstance = new NamedWindowRootViewInstance(processor.getRootView(), agentInstanceContext, processor.getEventTableIndexMetadata());
        tailViewInstance = new NamedWindowTailViewInstance(rootViewInstance, processor.getTailView(), processor, agentInstanceContext);
        rootViewInstance.setDataWindowContents(tailViewInstance);   // for iteration used for delete without index
    }

    public NamedWindowRootViewInstance getRootViewInstance() {
        return rootViewInstance;
    }

    public NamedWindowTailViewInstance getTailViewInstance() {
        return tailViewInstance;
    }

    public void destroy() {
        tailViewInstance.destroy();
        rootViewInstance.destroy();
    }

    public IndexMultiKey[] getIndexDescriptors() {
        return rootViewInstance.getIndexes();
    }

    public void removeIndex(IndexMultiKey index) {
        rootViewInstance.getIndexRepository().removeIndex(index);
    }

    public long getCountDataWindow() {
        return tailViewInstance.getNumberOfEvents();
    }

    public void removeExplicitIndex(String indexName) {
        rootViewInstance.getIndexRepository().removeExplicitIndex(indexName);
    }
}
