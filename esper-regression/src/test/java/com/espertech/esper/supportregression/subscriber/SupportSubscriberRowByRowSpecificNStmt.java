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
package com.espertech.esper.supportregression.subscriber;

import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;

public class SupportSubscriberRowByRowSpecificNStmt extends SupportSubscriberRowByRowSpecificBase {
    public SupportSubscriberRowByRowSpecificNStmt() {
        super(false);
    }

    public void update(String theString, int intPrimitive) {
        addIndication(new Object[]{theString, intPrimitive});
    }

    public void update(int wideByte, long wideInt, double wideLong, double wideFloat) {
        addIndication(new Object[]{wideByte, wideInt, wideLong, wideFloat});
    }

    public void update(SupportBean supportBean) {
        addIndication(new Object[]{supportBean});
    }

    public void update(SupportBean supportBean, int value1, String value2) {
        addIndication(new Object[]{supportBean, value1, value2});
    }

    public void update(SupportBeanComplexProps.SupportBeanSpecialGetterNested n,
                       SupportBeanComplexProps.SupportBeanSpecialGetterNestedNested nn) {
        addIndication(new Object[]{n, nn});
    }

    public void update(String theString, SupportEnum supportEnum) {
        addIndication(new Object[]{theString, supportEnum});
    }

    public void update(String nullableValue, Long longBoxed) {
        addIndication(new Object[]{nullableValue, longBoxed});
    }

    public void update(String value, SupportMarketDataBean s1, SupportBean s0) {
        addIndication(new Object[]{value, s1, s0});
    }

    public void update(SupportBean s0, SupportMarketDataBean s1) {
        addIndication(new Object[]{s0, s1});
    }
}
