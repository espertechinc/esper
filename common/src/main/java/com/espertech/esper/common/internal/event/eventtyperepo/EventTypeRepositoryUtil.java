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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeWithSupertype;
import com.espertech.esper.common.internal.util.GraphCircularDependencyException;
import com.espertech.esper.common.internal.util.GraphUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryMapTypeUtil.toTypesReferences;

public class EventTypeRepositoryUtil {
    public static List<String> getCreationOrder(Set<String> firstSet, Set<String> secondSet, Map<String, ? extends ConfigurationCommonEventTypeWithSupertype> configurations) {
        List<String> creationOrder = new ArrayList<>();
        creationOrder.addAll(firstSet);
        creationOrder.addAll(secondSet);

        Set<String> dependentOrder;
        try {
            Map<String, Set<String>> typesReferences = toTypesReferences(configurations);
            dependentOrder = GraphUtil.getTopDownOrder(typesReferences);
        } catch (GraphCircularDependencyException e) {
            throw new ConfigurationException("Error configuring event types, dependency graph between map type names is circular: " + e.getMessage(), e);
        }

        if (dependentOrder.isEmpty() || dependentOrder.size() < 2) {
            return creationOrder;
        }

        String[] dependents = dependentOrder.toArray(new String[0]);
        for (int i = 1; i < dependents.length; i++) {
            int indexSuper = creationOrder.indexOf(dependents[i - 1]);
            int indexSub = creationOrder.indexOf(dependents[i]);
            if (indexSuper == -1 || indexSub == -1) {
                continue;
            }
            if (indexSuper > indexSub) {
                creationOrder.remove(indexSub);
                if (indexSuper == creationOrder.size()) {
                    creationOrder.add(dependents[i]);
                } else {
                    creationOrder.add(indexSuper + 1, dependents[i]);
                }
            }
        }

        return creationOrder;
    }
}
