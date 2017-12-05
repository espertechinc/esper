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
package com.espertech.esper.epl.spec;

import com.espertech.esper.collection.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PluggableObjectRegistryImpl implements PluggableObjectRegistry {
    private PluggableObjectCollection[] collections;

    public PluggableObjectRegistryImpl(PluggableObjectCollection[] collections) {
        this.collections = collections;
    }

    public Pair<Class, PluggableObjectEntry> lookup(String nameSpace, String name) {

        // Handle namespace-provided
        if (nameSpace != null) {
            for (int i = 0; i < collections.length; i++) {
                Map<String, Pair<Class, PluggableObjectEntry>> names = collections[i].getPluggables().get(nameSpace);
                if (names == null) {
                    continue;
                }
                Pair<Class, PluggableObjectEntry> entry = names.get(name);
                if (entry == null) {
                    continue;
                }
                return entry;
            }
            return null;
        }

        // Handle namespace-not-provided
        Set<String> entriesDuplicate = null;
        Map.Entry<String, Pair<Class, PluggableObjectEntry>> found = null;
        for (int i = 0; i < collections.length; i++) {
            for (Map.Entry<String, Map<String, Pair<Class, PluggableObjectEntry>>> collEntry : collections[i].getPluggables().entrySet()) {
                for (Map.Entry<String, Pair<Class, PluggableObjectEntry>> viewEntry : collEntry.getValue().entrySet()) {
                    if (viewEntry.getKey().equals(name)) {
                        if (found != null) {
                            if (entriesDuplicate == null) {
                                entriesDuplicate = new HashSet<>();
                            }
                            entriesDuplicate.add(viewEntry.getKey());
                        } else {
                            found = viewEntry;
                        }
                    }
                }
            }
        }

        if (entriesDuplicate != null) {
            entriesDuplicate.add(found.getKey());
            throw new IllegalStateException("Duplicate entries for '" + name + "' found in namespaces " + Arrays.toString(entriesDuplicate.toArray()));
        }

        return found == null ? null : found.getValue();
    }
}
