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

import com.espertech.esper.client.EventBean;

import java.util.Collection;

/**
 * Listener to filter activity.
 */
public interface FilterServiceListener {
    /**
     * Indicates an event being filtered.
     *
     * @param theEvent    event
     * @param matches     matches found
     * @param statementId optional statement id if for a statement
     */
    public void filtering(EventBean theEvent, Collection<FilterHandle> matches, Integer statementId);
}