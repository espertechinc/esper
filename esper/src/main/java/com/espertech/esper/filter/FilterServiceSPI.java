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
package com.espertech.esper.filter;

import java.util.Set;

/**
 * Service provider interface for filter service.
 */
public interface FilterServiceSPI extends FilterService {
    public boolean isSupportsTakeApply();

    /**
     * Take a set of statements of out the active filters, returning a save-set of filters.
     *
     * @param statementId statement ids to remove
     * @return filters
     */
    public FilterSet take(Set<Integer> statementId);

    /**
     * Apply a set of previously taken filters.
     *
     * @param filterSet to apply
     */
    public void apply(FilterSet filterSet);

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

    public void acquireWriteLock();

    public void releaseWriteLock();

    /**
     * Initialization is optional and provides a chance to preload things after statements are available.
     */
    public void init();
}