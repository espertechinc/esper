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
package com.espertech.esper.common.internal.epl.agg.method.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;

public class AggregatorMethodFactoryContext {
    private final int col;
    private final CodegenCtor rowCtor;
    private final CodegenMemberCol membersColumnized;
    private final CodegenClassScope classScope;

    public AggregatorMethodFactoryContext(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        this.col = col;
        this.rowCtor = rowCtor;
        this.membersColumnized = membersColumnized;
        this.classScope = classScope;
    }

    public int getCol() {
        return col;
    }

    public CodegenCtor getRowCtor() {
        return rowCtor;
    }

    public CodegenMemberCol getMembersColumnized() {
        return membersColumnized;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }
}
