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

public class JsonEventUnderlyingValueIterator implements Iterator<Object> {
    private final JsonEventObjectBase jeu;
    private final Iterator<Object> valuesIter;
    private int count;

    public JsonEventUnderlyingValueIterator(JsonEventObjectBase jeu, Iterator<Object> valuesIter) {
        this.jeu = jeu;
        this.valuesIter = valuesIter;
    }

    public boolean hasNext() {
        return count < jeu.getNativeSize() || valuesIter.hasNext();
    }

    public Object next() {
        if (count < jeu.getNativeSize()) {
            return jeu.getNativeValue(count++);
        }
        return valuesIter.next();
    }
}
