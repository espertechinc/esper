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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.hook.type.SQLOutputRowConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowTypeContext;
import com.espertech.esper.common.client.hook.type.SQLOutputRowValueContext;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;

import java.util.ArrayList;
import java.util.List;

public class SupportSQLOutputRowConversion implements SQLOutputRowConversion {
    private static List<SQLOutputRowTypeContext> typeContexts;
    private static List<SQLOutputRowValueContext> valueContexts;

    static {
        reset();
    }

    public static void reset() {
        typeContexts = new ArrayList<SQLOutputRowTypeContext>();
        valueContexts = new ArrayList<SQLOutputRowValueContext>();
    }

    public static List<SQLOutputRowTypeContext> getTypeContexts() {
        return typeContexts;
    }

    public static List<SQLOutputRowValueContext> getValueContexts() {
        return valueContexts;
    }

    public EPTypeClass getOutputRowType(SQLOutputRowTypeContext sqlOutputRowTypeContext) {
        typeContexts.add(sqlOutputRowTypeContext);
        return ClassHelperGenericType.getClassEPType(SupportBean.class);
    }

    public Object getOutputRow(SQLOutputRowValueContext rowContext) {
        int myint = (Integer) rowContext.getValues().get("myint");
        if (myint == 90) {
            return null;
        }
        valueContexts.add(rowContext);
        return new SupportBean(">" + myint + "<", 99000 + myint);
    }
}
