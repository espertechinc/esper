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
package com.espertech.esper.pattern;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.MatchedEventMap;

import java.util.Collection;
import java.util.Set;

public class PatternConsumptionUtil {

    public static boolean containsEvent(Set<EventBean> matchEvent, MatchedEventMap beginState) {
        if (beginState == null) {
            return false;
        }
        Object[] partial = beginState.getMatchingEvents();
        boolean quit = false;
        for (Object aPartial : partial) {
            if (aPartial == null) {
                continue;
            } else if (aPartial instanceof EventBean) {
                if (matchEvent.contains(aPartial)) {
                    quit = true;
                    break;
                }
            } else if (aPartial instanceof EventBean[]) {
                EventBean[] events = (EventBean[]) aPartial;
                for (EventBean event : events) {
                    if (matchEvent.contains(event)) {
                        quit = true;
                        break;
                    }
                }
            }
            if (quit) {
                break;
            }
        }
        return quit;
    }

    public static void childNodeRemoveMatches(Set<EventBean> matchEvent, Collection<? extends EvalStateNode> evalStateNodes) {
        EvalStateNode[] nodesArray = evalStateNodes.toArray(new EvalStateNode[evalStateNodes.size()]);
        for (EvalStateNode child : nodesArray) {
            child.removeMatch(matchEvent);
        }
    }
}
