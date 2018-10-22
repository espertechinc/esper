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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the equals (=) operator.
 * The implementation is based on a regular HashMap.
 */
public final class FilterParamIndexNotEqualsIs extends FilterParamIndexNotEqualsBase {
    public FilterParamIndexNotEqualsIs(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(lookupable, readWriteLock, FilterOperator.IS_NOT);
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        Object attributeValue = lookupable.getGetter().get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, attributeValue);
        }

        // Look up in hashtable
        constantsMapRWLock.readLock().lock();
        try {
            for (Map.Entry<Object, EventEvaluator> entry : constantsMap.entrySet()) {
                if (entry.getKey() == null) {
                    if (attributeValue != null) {
                        entry.getValue().matchEvent(theEvent, matches);
                    }
                    continue;
                }

                if (!entry.getKey().equals(attributeValue)) {
                    entry.getValue().matchEvent(theEvent, matches);
                }
            }
        } finally {
            constantsMapRWLock.readLock().unlock();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }
}
