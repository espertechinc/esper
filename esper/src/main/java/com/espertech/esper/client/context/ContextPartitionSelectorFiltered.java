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

/**
 * Selects context partitions by receiving a context partition identifier for interrogation.
 */
public interface ContextPartitionSelectorFiltered extends ContextPartitionSelector {
    /**
     * Filter function should return true or false to indicate
     * interest in this context partition.
     * <p>
     * Do not hold on to ContextIdentifier instance between calls.
     * The engine may reused an reassing values to this object.
     * </p>
     *
     * @param contextPartitionIdentifier provides context partition information, may
     * @return true to pass filter, false to reject
     */
    public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier);
}
