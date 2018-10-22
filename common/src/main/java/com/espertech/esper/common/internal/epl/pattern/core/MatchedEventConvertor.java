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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Converts from a map of prior matching events to a events per stream for resultion by expressions.
 */
public interface MatchedEventConvertor {
    /**
     * Converts pattern matching events to events per stream.
     *
     * @param events pattern partial matches
     * @return events per stream
     */
    EventBean[] convert(MatchedEventMap events);
}