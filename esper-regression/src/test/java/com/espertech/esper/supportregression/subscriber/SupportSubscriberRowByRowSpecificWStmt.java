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

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;

public class SupportSubscriberRowByRowSpecificWStmt extends SupportSubscriberRowByRowSpecificBase {
    public SupportSubscriberRowByRowSpecificWStmt() {
        super(true);
    }

    public void update(EPStatement statement, String theString, int intPrimitive) {
        addIndication(statement, new Object[]{theString, intPrimitive});
    }

    public void update(EPStatement statement, int wideByte, long wideInt, double wideLong, double wideFloat) {
        addIndication(statement, new Object[]{wideByte, wideInt, wideLong, wideFloat});
    }

    public void update(EPStatement statement, SupportBean supportBean) {
        addIndication(statement, new Object[]{supportBean});
    }

    public void update(EPStatement statement, SupportBean supportBean, int value1, String value2) {
        addIndication(statement, new Object[]{supportBean, value1, value2});
    }

    public void update(EPStatement statement, SupportBeanComplexProps.SupportBeanSpecialGetterNested n,
                       SupportBeanComplexProps.SupportBeanSpecialGetterNestedNested nn) {
        addIndication(statement, new Object[]{n, nn});
    }

    public void update(EPStatement statement, String theString, SupportEnum supportEnum) {
        addIndication(statement, new Object[]{theString, supportEnum});
    }

    public void update(EPStatement statement, String nullableValue, Long longBoxed) {
        addIndication(statement, new Object[]{nullableValue, longBoxed});
    }

    public void update(EPStatement statement, String value, SupportMarketDataBean s1, SupportBean s0) {
        addIndication(statement, new Object[]{value, s1, s0});
    }

    public void update(EPStatement statement, SupportBean s0, SupportMarketDataBean s1) {
        addIndication(statement, new Object[]{s0, s1});
    }
}
