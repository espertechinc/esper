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
package com.espertech.esper.common.internal.epl.rowrecog.state;

import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;

import java.util.Iterator;
import java.util.List;

public interface RowRecogPartitionState {
    public RowRecogStateRandomAccess getRandomAccess();

    public Iterator<RowRecogNFAStateEntry> getCurrentStatesIterator();

    public void setCurrentStates(List<RowRecogNFAStateEntry> currentStates);

    public Object getOptionalKeys();

    public int getNumStates();

    public List<RowRecogNFAStateEntry> getCurrentStatesForPrint();

    public boolean isEmptyCurrentState();
}
