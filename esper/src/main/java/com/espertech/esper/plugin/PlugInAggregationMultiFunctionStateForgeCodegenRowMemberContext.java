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
package com.espertech.esper.plugin;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.epl.expression.accessagg.ExprPlugInAggMultiFunctionNodeFactory;

public class PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext {
    private final ExprPlugInAggMultiFunctionNodeFactory parent;
    private final int column;
    private final CodegenCtor ctor;
    private final CodegenMembersColumnized membersColumnized;
    private final CodegenClassScope classScope;

    public PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext(ExprPlugInAggMultiFunctionNodeFactory parent, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        this.parent = parent;
        this.column = column;
        this.ctor = ctor;
        this.membersColumnized = membersColumnized;
        this.classScope = classScope;
    }

    public ExprPlugInAggMultiFunctionNodeFactory getParent() {
        return parent;
    }

    public int getColumn() {
        return column;
    }

    public CodegenCtor getCtor() {
        return ctor;
    }

    public CodegenMembersColumnized getMembersColumnized() {
        return membersColumnized;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }
}
