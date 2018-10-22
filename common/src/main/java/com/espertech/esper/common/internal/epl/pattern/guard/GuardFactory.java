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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Interface for a factory for {@link Guard} instances.
 */
public interface GuardFactory {

    /**
     * Constructs a guard instance.
     *
     * @param context    - services for use by guard
     * @param beginState - the prior matching events
     * @param quitable   - to use for indicating the guard has quit
     * @param guardState - state node for guard
     * @return guard instance
     */
    public Guard makeGuard(PatternAgentInstanceContext context,
                           MatchedEventMap beginState,
                           Quitable quitable,
                           Object guardState);
}
