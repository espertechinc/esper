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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.view.View;

/**
 * Update dispatch view to indicate statement results to listeners.
 */
public interface UpdateDispatchView extends View {
    /**
     * Convenience method that accepts a pair of new and old data
     * as this is the most treated unit.
     *
     * @param result is new data (insert stream) and old data (remove stream)
     */
    public void newResult(UniformPair<EventBean[]> result);
}
