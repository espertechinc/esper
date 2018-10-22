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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInnerClass;

import java.util.List;

public class AggregationServiceFactoryMakeResult {
    private final CodegenMethod initMethod;
    private final List<CodegenInnerClass> innerClasses;

    public AggregationServiceFactoryMakeResult(CodegenMethod initMethod, List<CodegenInnerClass> innerClasses) {
        this.initMethod = initMethod;
        this.innerClasses = innerClasses;
    }

    public CodegenMethod getInitMethod() {
        return initMethod;
    }

    public List<CodegenInnerClass> getInnerClasses() {
        return innerClasses;
    }
}
