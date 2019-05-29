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
package com.espertech.esper.common.internal.event.json.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonEventUnderlyingEntrySetIterator implements Iterator<Map.Entry<String, Object>> {
    private final JsonEventObjectBase jeu;
    private final Iterator<Map.Entry<String, Object>> mapIter;
    private int count;

    public JsonEventUnderlyingEntrySetIterator(JsonEventObjectBase jeu, Set<Map.Entry<String, Object>> entrySet) {
        this.jeu = jeu;
        this.mapIter = entrySet.iterator();
    }

    public boolean hasNext() {
        return count < jeu.getNativeSize() || mapIter.hasNext();
    }

    public Map.Entry<String, Object> next() {
        if (count < jeu.getNativeSize()) {
            return jeu.getNativeEntry(count++);
        }
        return mapIter.next();
    }
}
