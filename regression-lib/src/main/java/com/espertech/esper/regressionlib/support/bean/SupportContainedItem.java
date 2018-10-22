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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportContainedItem implements Serializable {
    private final String itemId;
    private final String selected;

    public SupportContainedItem(String itemId, String selected) {
        this.itemId = itemId;
        this.selected = selected;
    }

    public String getItemId() {
        return itemId;
    }

    public String getSelected() {
        return selected;
    }
}
