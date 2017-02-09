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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Strategy for looking up, in some sort of table or index, or a set of events, potentially based on the
 * events properties, and returning a set of matched events.
 */
public interface SubordTableLookupStrategyFactory {
    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw);

    public String toQueryPlan();
}
