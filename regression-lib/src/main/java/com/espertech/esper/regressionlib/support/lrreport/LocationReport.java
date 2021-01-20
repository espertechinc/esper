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
package com.espertech.esper.regressionlib.support.lrreport;

import java.io.Serializable;
import java.util.List;

public class LocationReport implements Serializable {

    private static final long serialVersionUID = -527984670941127607L;
    private List<Item> items;

    public LocationReport(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }
}
