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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;

public class AggregationAccessorForgeGetCodegenContext {
    private final int column;
    private final CodegenClassScope classScope;
    private final AggregationStateFactoryForge accessStateForge;
    private final CodegenMethodNode method;
    private final CodegenNamedMethods namedMethods;

    public AggregationAccessorForgeGetCodegenContext(int column, CodegenClassScope classScope, AggregationStateFactoryForge accessStateForge, CodegenMethodNode method, CodegenNamedMethods namedMethods) {
        this.column = column;
        this.classScope = classScope;
        this.accessStateForge = accessStateForge;
        this.method = method;
        this.namedMethods = namedMethods;
    }

    public int getColumn() {
        return column;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }

    public AggregationStateFactoryForge getAccessStateForge() {
        return accessStateForge;
    }

    public CodegenMethodNode getMethod() {
        return method;
    }

    public CodegenNamedMethods getNamedMethods() {
        return namedMethods;
    }
}
