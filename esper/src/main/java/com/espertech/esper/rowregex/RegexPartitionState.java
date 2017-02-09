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

import java.util.Iterator;
import java.util.List;

public interface RegexPartitionState {
    public RegexPartitionStateRandomAccess getRandomAccess();

    public Iterator<RegexNFAStateEntry> getCurrentStatesIterator();

    public void setCurrentStates(List<RegexNFAStateEntry> currentStates);

    public Object getOptionalKeys();

    public int getNumStates();

    public List<RegexNFAStateEntry> getCurrentStatesForPrint();

    public boolean isEmptyCurrentState();
}
