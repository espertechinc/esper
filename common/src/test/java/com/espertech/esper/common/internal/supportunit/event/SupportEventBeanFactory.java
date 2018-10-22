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
package com.espertech.esper.common.internal.supportunit.event;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.common.internal.supportunit.bean.*;

public class SupportEventBeanFactory {

    public static EventBean createObject(Object theEvent) {
        if (theEvent instanceof SupportBean) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_EVENTTTPE);
        } else if (theEvent instanceof SupportBean_S0) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_S0_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanString) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANSTRING_EVENTTTPE);
        } else if (theEvent instanceof SupportBean_A) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_A_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanComplexProps) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANCOMPLEXPROPS_EVENTTTPE);
        } else if (theEvent instanceof SupportLegacyBean) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTLEGACYBEAN_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanCombinedProps) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANCOMBINEDPROPS_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanPropertyNames) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANPROPERTYNAMES_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanIterableProps) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANITERABLEPROPS_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanIterablePropsContainer) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANITERABLEPROPSCONTAINER_EVENTTYPE);
        } else if (theEvent instanceof SupportBeanSimple) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANSIMPLE_EVENTTTPE);
        } else {
            throw new UnsupportedOperationException("Unexpected type " + theEvent.getClass());
        }
    }

    public static EventBean[] makeEvents(String[] ids) {
        EventBean[] events = new EventBean[ids.length];
        for (int i = 0; i < events.length; i++) {
            SupportBean bean = new SupportBean();
            bean.setTheString(ids[i]);
            events[i] = createObject(bean);
        }
        return events;
    }
}
