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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.internal.context.controller.core.ContextControllerDetail;

public class ContextControllerDetailHash implements ContextControllerDetail {
    private ContextControllerDetailHashItem[] items;
    private int granularity;
    private boolean preallocate;

    public ContextControllerDetailHashItem[] getItems() {
        return items;
    }

    public void setItems(ContextControllerDetailHashItem[] items) {
        this.items = items;
    }

    public int getGranularity() {
        return granularity;
    }

    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    public boolean isPreallocate() {
        return preallocate;
    }

    public void setPreallocate(boolean preallocate) {
        this.preallocate = preallocate;
    }
}
