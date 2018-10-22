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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

import java.util.Collection;

public class ContextControllerKeyedFilterEntryNoInit extends ContextControllerKeyedFilterEntry {

    public ContextControllerKeyedFilterEntryNoInit(ContextControllerKeyedImpl callback, IntSeqKey controllerPath, Object[] parentPartitionKeys, ContextControllerDetailKeyedItem item) {
        super(callback, controllerPath, item, parentPartitionKeys);
        start(item.getFilterSpecActivatable());
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        callback.matchFound(item, theEvent, controllerPath, item.getAliasName());
    }

    public void destroy() {
        stop(item.getFilterSpecActivatable());
    }
}
