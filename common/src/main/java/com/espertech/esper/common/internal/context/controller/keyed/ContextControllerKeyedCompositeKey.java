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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.internal.collection.IntSeqKey;

public class ContextControllerKeyedCompositeKey {
    private final IntSeqKey path;
    private final Object key;

    public ContextControllerKeyedCompositeKey(IntSeqKey path, Object key) {
        this.path = path;
        this.key = key;
    }

    public IntSeqKey getPath() {
        return path;
    }

    public Object getKey() {
        return key;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextControllerKeyedCompositeKey that = (ContextControllerKeyedCompositeKey) o;

        if (!path.equals(that.path)) return false;
        return key != null ? key.equals(that.key) : that.key == null;
    }

    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
