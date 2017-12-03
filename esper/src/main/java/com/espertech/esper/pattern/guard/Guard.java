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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * Guard instances inspect a matched events and makes a determination on whether to let it pass or not.
 */
public interface Guard {
    /**
     * Start the guard operation.
     */
    public void startGuard();

    /**
     * Called when sub-expression quits, or when the pattern stopped.
     */
    public void stopGuard();

    /**
     * Returns true if inspection shows that the match events can pass, or false to not pass.
     *
     * @param matchEvent is the map of matching events
     * @return true to pass, false to not pass
     */
    public boolean inspect(MatchedEventMap matchEvent);

    public void accept(EventGuardVisitor visitor);
}
