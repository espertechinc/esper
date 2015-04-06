/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.spec;

import com.espertech.esper.collection.Pair;

import java.util.Map;

public class PluggableObjectRegistryImpl implements PluggableObjectRegistry
{
    private PluggableObjectCollection[] collections;

    public PluggableObjectRegistryImpl(PluggableObjectCollection[] collections) {
        this.collections = collections;
    }

    public Pair<Class, PluggableObjectEntry> lookup(String nameSpace, String name) {

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
}
