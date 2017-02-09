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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;

/**
 * Service for holding partition state.
 */
public interface RegexPartitionStateRepo {
    /**
     * Return state for key or create state if not found.
     *
     * @param key to look up
     * @return state
     */
    public RegexPartitionState getState(Object key);

    /**
     * Return state for event or create state if not found.
     *
     * @param theEvent  to look up
     * @param isCollect true if a collection of unused state can occur
     * @return state
     */
    public RegexPartitionState getState(EventBean theEvent, boolean isCollect);

    /**
     * Remove old events from the state, applicable for "prev" function and partial NFA state.
     *
     * @param events  to remove
     * @param isEmpty indicator if there are not matches
     * @param found   indicator if any partial matches exist to be deleted
     * @return number removed
     */
    public int removeOld(EventBean[] events, boolean isEmpty, boolean[] found);

    /**
     * Copy state for iteration.
     *
     * @param forOutOfOrderReprocessing indicator whether we are processing out-of-order events
     * @return copied state
     */
    public RegexPartitionStateRepo copyForIterate(boolean forOutOfOrderReprocessing);

    public void removeState(Object partitionKey);

    public void accept(EventRowRegexNFAViewServiceVisitor visitor);

    public boolean isPartitioned();

    public int getStateCount();

    public int incrementAndGetEventSequenceNum();

    public void setEventSequenceNum(int num);

    public RegexPartitionStateRepoScheduleState getScheduleState();

    public void destroy();
}
