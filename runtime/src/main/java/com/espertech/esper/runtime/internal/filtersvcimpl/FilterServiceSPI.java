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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.internal.filtersvc.FilterService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Service provider interface for filter service.
 */
public interface FilterServiceSPI extends FilterService {
    /**
     * Get a set of statements of out the active filters, returning filters.
     *
     * @param statementId statement ids to remove
     * @return filters
     */
    public Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> get(Set<Integer> statementId);

    /**
     * Add activity listener.void acquireWriteLock();
     *
     * @param filterServiceListener to add
     */
    public void addFilterServiceListener(FilterServiceListener filterServiceListener);

    /**
     * Remove activity listener.
     *
     * @param filterServiceListener to remove
     */
    public void removeFilterServiceListener(FilterServiceListener filterServiceListener);

    public int getFilterCountApprox();

    public int getCountTypes();

    /**
     * Initialization is optional and provides a chance to preload things after statements are available.
     *
     * @param availableTypes type information
     */
    public void init(Supplier<Collection<EventType>> availableTypes);
}