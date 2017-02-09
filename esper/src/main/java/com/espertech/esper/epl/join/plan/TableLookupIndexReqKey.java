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
package com.espertech.esper.epl.join.plan;

public class TableLookupIndexReqKey {
    private final String name;
    private final String tableName;

    public TableLookupIndexReqKey(String name) {
        this(name, null);
    }

    public TableLookupIndexReqKey(String name, String tableName) {
        this.name = name;
        this.tableName = tableName;
    }

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableLookupIndexReqKey that = (TableLookupIndexReqKey) o;

        if (tableName != null ? !tableName.equals(that.tableName) : that.tableName != null)
            return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }

    public String toString() {
        if (tableName == null) {
            return name;
        } else {
            return "table '" + tableName + "' index '" + name + "'";
        }
    }
}
