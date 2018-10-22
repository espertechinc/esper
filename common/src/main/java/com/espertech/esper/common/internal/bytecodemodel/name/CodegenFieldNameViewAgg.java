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

public class CodegenFieldNameViewAgg implements CodegenFieldName {
    private final int streamNumber;

    public CodegenFieldNameViewAgg(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public String getName() {
        return CodegenPackageScopeNames.aggView(streamNumber);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenFieldNameViewAgg that = (CodegenFieldNameViewAgg) o;

        return streamNumber == that.streamNumber;
    }

    public int hashCode() {
        return streamNumber;
    }
}
