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

import com.espertech.esper.common.client.EventBean;

/**
 * Keyed-context partition key for the case when there are init-conditions and we must keep the initiating event
 */
public class ContextControllerKeyedPartitionKeyWInit {
    private final Object getterKey;
    private final String optionalInitAsName;
    private final EventBean optionalInitBean;

    public ContextControllerKeyedPartitionKeyWInit(Object getterKey, String optionalInitAsName, EventBean optionalInitBean) {
        this.getterKey = getterKey;
        this.optionalInitAsName = optionalInitAsName;
        this.optionalInitBean = optionalInitBean;
    }

    public Object getGetterKey() {
        return getterKey;
    }

    public String getOptionalInitAsName() {
        return optionalInitAsName;
    }

    public EventBean getOptionalInitBean() {
        return optionalInitBean;
    }
}
