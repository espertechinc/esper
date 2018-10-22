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
package com.espertech.esper.common.internal.bytecodemodel.name;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScopeNames;

public class CodegenFieldNameTableAccess implements CodegenFieldName {
    private final int tableAccessNumber;

    public CodegenFieldNameTableAccess(int tableAccessNumber) {
        this.tableAccessNumber = tableAccessNumber;
    }

    public String getName() {
        return CodegenPackageScopeNames.tableAccessResultFuture(tableAccessNumber);
    }

    public int getTableAccessNumber() {
        return tableAccessNumber;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenFieldNameTableAccess that = (CodegenFieldNameTableAccess) o;

        return tableAccessNumber == that.tableAccessNumber;
    }

    public int hashCode() {
        return tableAccessNumber;
    }
}
