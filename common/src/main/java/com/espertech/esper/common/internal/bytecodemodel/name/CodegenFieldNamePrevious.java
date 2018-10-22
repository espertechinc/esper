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

public class CodegenFieldNamePrevious implements CodegenFieldName {
    private final int streamNumber;

    public CodegenFieldNamePrevious(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public String getName() {
        return CodegenPackageScopeNames.previous(streamNumber);
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenFieldNamePrevious previous = (CodegenFieldNamePrevious) o;

        return streamNumber == previous.streamNumber;
    }

    public int hashCode() {
        return streamNumber;
    }
}
