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

import java.util.List;

public class ContextDetailHash implements ContextDetail {

    private static final long serialVersionUID = -7754347180148095977L;
    private final List<ContextDetailHashItem> items;
    private final int granularity;
    private final boolean preallocate;

    public ContextDetailHash(List<ContextDetailHashItem> items, int granularity, boolean preallocate) {
        this.items = items;
        this.preallocate = preallocate;
        this.granularity = granularity;
    }

    public List<ContextDetailHashItem> getItems() {
        return items;
    }

    public boolean isPreallocate() {
        return preallocate;
    }

    public int getGranularity() {
        return granularity;
    }
}
