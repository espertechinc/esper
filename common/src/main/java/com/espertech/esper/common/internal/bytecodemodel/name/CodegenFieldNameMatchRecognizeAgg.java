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

public class CodegenFieldNameMatchRecognizeAgg implements CodegenFieldName {
    private final int aggregationNumber;

    public CodegenFieldNameMatchRecognizeAgg(int aggregationNumber) {
        this.aggregationNumber = aggregationNumber;
    }

    public String getName() {
        return CodegenPackageScopeNames.aggregationMatchRecognize(aggregationNumber);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenFieldNameMatchRecognizeAgg that = (CodegenFieldNameMatchRecognizeAgg) o;

        return aggregationNumber == that.aggregationNumber;
    }

    public int hashCode() {
        return aggregationNumber;
    }
}
