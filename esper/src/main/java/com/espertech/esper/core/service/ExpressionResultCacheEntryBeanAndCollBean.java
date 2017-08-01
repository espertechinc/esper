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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;

import java.util.Collection;

/**
 * Cache entry bean-to-collection-of-bean.
 */
public class ExpressionResultCacheEntryBeanAndCollBean {
    private EventBean reference;
    private Collection<EventBean> result;

    public ExpressionResultCacheEntryBeanAndCollBean(EventBean reference, Collection<EventBean> result) {
        this.reference = reference;
        this.result = result;
    }

    public EventBean getReference() {
        return reference;
    }

    public void setReference(EventBean reference) {
        this.reference = reference;
    }

    public Collection<EventBean> getResult() {
        return result;
    }

    public void setResult(Collection<EventBean> result) {
        this.result = result;
    }
}
