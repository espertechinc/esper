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

import com.espertech.esper.client.hook.SQLColumnTypeContext;
import com.espertech.esper.client.hook.SQLColumnTypeConversion;
import com.espertech.esper.client.hook.SQLColumnValueContext;
import com.espertech.esper.client.hook.SQLInputParameterContext;

import java.util.ArrayList;
import java.util.List;

public class SupportSQLColumnTypeConversion implements SQLColumnTypeConversion {
    private static List<SQLColumnTypeContext> typeContexts;
    private static List<SQLColumnValueContext> valueContexts;
    private static List<SQLInputParameterContext> paramContexts;

    static {
        reset();
    }

    public static void reset() {
        typeContexts = new ArrayList<SQLColumnTypeContext>();
        valueContexts = new ArrayList<SQLColumnValueContext>();
        paramContexts = new ArrayList<SQLInputParameterContext>();
    }

    public static List<SQLColumnTypeContext> getTypeContexts() {
        return typeContexts;
    }

    public static List<SQLColumnValueContext> getValueContexts() {
        return valueContexts;
    }

    public static List<SQLInputParameterContext> getParamContexts() {
        return paramContexts;
    }

    public Class getColumnType(SQLColumnTypeContext sqlColumnTypeContext) {
        typeContexts.add(sqlColumnTypeContext);
        return Boolean.class;
    }

    public Object getColumnValue(SQLColumnValueContext valueContext) {
        valueContexts.add(valueContext);
        return ((Integer) valueContext.getColumnValue()) >= 50;
    }

    public Object getParameterValue(SQLInputParameterContext inputParameterContext) {
        paramContexts.add(inputParameterContext);
        if (inputParameterContext.getParameterValue() instanceof String) {
            return Integer.parseInt(inputParameterContext.getParameterValue().toString().substring(1));
        }
        return inputParameterContext.getParameterValue();
    }
}
