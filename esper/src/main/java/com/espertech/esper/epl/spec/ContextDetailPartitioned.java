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

public class ContextDetailPartitioned implements ContextDetail {

    private static final long serialVersionUID = -7754347180148095977L;

    private final List<ContextDetailPartitionItem> items;
    private List<ContextDetailConditionFilter> optionalInit;
    private ContextDetailCondition optionalTermination;

    public ContextDetailPartitioned(List<ContextDetailPartitionItem> items, List<ContextDetailConditionFilter> optionalInit, ContextDetailCondition optionalTermination) {
        this.items = items;
        this.optionalInit = optionalInit;
        this.optionalTermination = optionalTermination;
    }

    public List<ContextDetailPartitionItem> getItems() {
        return items;
    }

    public ContextDetailCondition getOptionalTermination() {
        return optionalTermination;
    }

    public void setOptionalTermination(ContextDetailCondition optionalTermination) {
        this.optionalTermination = optionalTermination;
    }

    public List<ContextDetailConditionFilter> getOptionalInit() {
        return optionalInit;
    }
}
