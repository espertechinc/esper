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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Observers observe and indicate other external events such as timing events.
 */
public interface EventObserver {
    /**
     * Start observing.
     */
    void startObserve();

    /**
     * Stop observing.
     */
    void stopObserve();

    void accept(EventObserverVisitor visitor);

    MatchedEventMap getBeginState();
}
