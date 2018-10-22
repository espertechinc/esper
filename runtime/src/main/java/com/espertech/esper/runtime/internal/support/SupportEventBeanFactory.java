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
package com.espertech.esper.runtime.internal.support;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.support.*;

public class SupportEventBeanFactory {

    public static EventBean createObject(Object theEvent) {
        if (theEvent instanceof SupportBean) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_EVENTTTPE);
        } else if (theEvent instanceof SupportBean_S0) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_S0_EVENTTTPE);
        } else if (theEvent instanceof SupportBean_A) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEAN_A_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanComplexProps) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANCOMPLEXPROPS_EVENTTTPE);
        } else if (theEvent instanceof SupportBeanSimple) {
            return new BeanEventBean(theEvent, SupportEventTypeFactory.SUPPORTBEANSIMPLE_EVENTTTPE);
        } else {
            throw new UnsupportedOperationException("Unexpected type " + theEvent.getClass());
        }
    }
}
