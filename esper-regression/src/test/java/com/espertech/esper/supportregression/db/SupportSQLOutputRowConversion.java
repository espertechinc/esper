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
package com.espertech.esper.supportregression.db;

import com.espertech.esper.client.hook.SQLOutputRowConversion;
import com.espertech.esper.client.hook.SQLOutputRowTypeContext;
import com.espertech.esper.client.hook.SQLOutputRowValueContext;
import com.espertech.esper.supportregression.bean.SupportBean;

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

    public Class getOutputRowType(SQLOutputRowTypeContext sqlOutputRowTypeContext) {
        typeContexts.add(sqlOutputRowTypeContext);
        return SupportBean.class;
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
