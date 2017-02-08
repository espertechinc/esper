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
package com.espertech.esper.dataflow.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OperatorDependencyUtil {

    public static Set<Integer> roots(Map<Integer, OperatorDependencyEntry> dependencyEntryMap) {
        Set<Integer> roots = new HashSet<Integer>();
        for (Map.Entry<Integer, OperatorDependencyEntry> entry : dependencyEntryMap.entrySet()) {
            if (entry.getValue().getIncoming().isEmpty()) {
                roots.add(entry.getKey());
            }
        }
        return roots;
    }
}
