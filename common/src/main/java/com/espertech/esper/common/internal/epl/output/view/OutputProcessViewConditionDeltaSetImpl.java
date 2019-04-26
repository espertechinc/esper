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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OutputProcessViewConditionDeltaSetImpl implements OutputProcessViewConditionDeltaSet {
    private final List<UniformPair<EventBean[]>> viewEventsList;
    private final List<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>> joinEventsSet;

    public OutputProcessViewConditionDeltaSetImpl(boolean isJoin) {
        if (isJoin) {
            joinEventsSet = new ArrayList<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>>();
            viewEventsList = Collections.emptyList();
        } else {
            viewEventsList = new ArrayList<UniformPair<EventBean[]>>();
            joinEventsSet = Collections.emptyList();
        }
    }

    public int getNumChangesetRows() {
        return Math.max(viewEventsList.size(), joinEventsSet.size());
    }

    public void addView(UniformPair<EventBean[]> uniformPair) {
        viewEventsList.add(uniformPair);
    }

    public void addJoin(UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> setUniformPair) {
        joinEventsSet.add(setUniformPair);
    }

    public void clear() {
        viewEventsList.clear();
        joinEventsSet.clear();
    }

    public void destroy() {
        clear();
    }

    public List<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>> getJoinEventsSet() {
        return joinEventsSet;
    }

    public List<UniformPair<EventBean[]>> getViewEventsSet() {
        return viewEventsList;
    }
}
