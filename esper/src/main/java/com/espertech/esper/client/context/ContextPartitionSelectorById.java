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
package com.espertech.esper.client.context;

import java.util.Set;

/**
 * Selects a context partition by providing the context partition id(s).
 */
public interface ContextPartitionSelectorById extends ContextPartitionSelector {

    /**
     * Return the context partition ids to select.
     *
     * @return id set
     */
    public Set<Integer> getContextPartitionIds();
}
