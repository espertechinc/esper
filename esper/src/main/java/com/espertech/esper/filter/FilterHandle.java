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

/**
 * Marker interface for use with {@link FilterService}. Implementations serve as a filter match values when
 * events match filters, and also serve to enter and remove a filter from the filter subscription set.
 */
public interface FilterHandle {
    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    public int getStatementId();
}

