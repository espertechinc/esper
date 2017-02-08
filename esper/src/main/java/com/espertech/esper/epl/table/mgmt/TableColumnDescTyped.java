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
package com.espertech.esper.epl.table.mgmt;

public class TableColumnDescTyped extends TableColumnDesc {
    private final Object unresolvedType;
    private final boolean key;

    public TableColumnDescTyped(int positionInDeclaration, String columnName, Object unresolvedType, boolean key) {
        super(positionInDeclaration, columnName);
        this.unresolvedType = unresolvedType;
        this.key = key;
    }

    public Object getUnresolvedType() {
        return unresolvedType;
    }

    public boolean isKey() {
        return key;
    }
}
