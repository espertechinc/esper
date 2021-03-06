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
package com.espertech.esper.common.internal.epl.enummethod.cache;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;

import java.util.Collection;

/**
 * Cache entry bean-to-collection-of-bean.
 */
public class ExpressionResultCacheEntryEventBeanArrayAndCollBean {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class);

    private EventBean[] reference;
    private Collection<EventBean> result;

    public ExpressionResultCacheEntryEventBeanArrayAndCollBean(EventBean[] reference, Collection<EventBean> result) {
        this.reference = reference;
        this.result = result;
    }

    public EventBean[] getReference() {
        return reference;
    }

    public void setReference(EventBean[] reference) {
        this.reference = reference;
    }

    public Collection<EventBean> getResult() {
        return result;
    }

    public void setResult(Collection<EventBean> result) {
        this.result = result;
    }
}
