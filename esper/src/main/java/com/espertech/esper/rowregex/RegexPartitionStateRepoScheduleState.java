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

import java.util.List;

/**
 * Service for holding schedule state.
 */
public interface RegexPartitionStateRepoScheduleState {
    boolean isEmpty();

    /**
     * Add entry returning true if the key did not exist.
     *
     * @param matchBeginTime key
     * @param state          entry
     * @return indicator
     */
    boolean putOrAdd(long matchBeginTime, RegexNFAStateEntry state);

    long firstKey();

    void removeAddRemoved(long matchBeginTime, List<RegexNFAStateEntry> foundStates);

    boolean containsKey(long matchBeginTime);

    /**
     * Find and remove operation, wherein removed items are added to the found list,
     * returning an indicator whether the item was found and removed
     *
     * @param matchBeginTime key
     * @param state          entry
     * @param foundStates    list to be added to
     * @return indicator whether any item was found and removed
     */
    boolean findRemoveAddToList(long matchBeginTime, RegexNFAStateEntry state, List<RegexNFAStateEntry> foundStates);
}
