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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;

import java.util.List;

public class AggregationRowCtorDesc {
    private final CodegenClassScope classScope;
    private final CodegenCtor rowCtor;
    private final List<CodegenTypedParam> rowMembers;
    private final CodegenNamedMethods namedMethods;

    public AggregationRowCtorDesc(CodegenClassScope classScope, CodegenCtor rowCtor, List<CodegenTypedParam> rowMembers, CodegenNamedMethods namedMethods) {
        this.classScope = classScope;
        this.rowCtor = rowCtor;
        this.rowMembers = rowMembers;
        this.namedMethods = namedMethods;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }

    public CodegenCtor getRowCtor() {
        return rowCtor;
    }

    public List<CodegenTypedParam> getRowMembers() {
        return rowMembers;
    }

    public CodegenNamedMethods getNamedMethods() {
        return namedMethods;
    }
}
