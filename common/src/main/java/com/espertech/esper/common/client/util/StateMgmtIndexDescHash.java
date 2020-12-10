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
package com.espertech.esper.common.client.util;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;

public class StateMgmtIndexDescHash {
    private final String[] indexedProps;
    private final MultiKeyClassRef multiKeyPlan;
    private final boolean unique;

    public StateMgmtIndexDescHash(String[] indexedProps, MultiKeyClassRef multiKeyPlan, boolean unique) {
        this.indexedProps = indexedProps;
        this.multiKeyPlan = multiKeyPlan;
        this.unique = unique;
    }

    public String[] getIndexedProps() {
        return indexedProps;
    }

    public MultiKeyClassRef getMultiKeyPlan() {
        return multiKeyPlan;
    }

    public boolean isUnique() {
        return unique;
    }
}
