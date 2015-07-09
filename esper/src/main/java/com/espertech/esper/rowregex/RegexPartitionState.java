/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface RegexPartitionState
{
    public RegexPartitionStateRandomAccessImpl getRandomAccess();
    public Iterator<RegexNFAStateEntry> getCurrentStatesIterator();
    public void setCurrentStates(List<RegexNFAStateEntry> currentStates);
    public Object getOptionalKeys();
    public void removeEventFromPrev(EventBean[] oldEvents);
    public void removeEventFromPrev(EventBean oldEvent);
    public boolean removeEventFromState(EventBean oldEvent);
    public int getNumStates();
    public void clearCurrentStates();
    public List<RegexNFAStateEntry> getCurrentStatesForPrint();
    public boolean isEmptyCurrentState();
}
