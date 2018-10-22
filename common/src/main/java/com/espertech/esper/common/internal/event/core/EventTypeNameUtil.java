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
package com.espertech.esper.common.internal.event.core;

public class EventTypeNameUtil {
    private final static String TABLE_INTERNAL_PREFIX = "table_internal_";

    public static String getWrapperInnerTypeName(String name) {
        return name + "_in";
    }

    public static String getTableInternalTypeName(String tableName) {
        return TABLE_INTERNAL_PREFIX + tableName;
    }

    public static String getTableNameFromInternalTypeName(String typeName) {
        return typeName.substring(TABLE_INTERNAL_PREFIX.length());
    }

    public static String getTablePublicTypeName(String tableName) {
        return "table_" + tableName;
    }

    public static String getAnonymousTypeNameExcludePlanHint() {
        return "exclude_plan_hint";
    }
}
