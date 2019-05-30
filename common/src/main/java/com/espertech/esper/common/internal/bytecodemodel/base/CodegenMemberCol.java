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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMemberWCol;

import java.util.LinkedHashMap;

public class CodegenMemberCol {
    private final LinkedHashMap<CodegenExpressionMemberWCol, Class> members = new LinkedHashMap<>();

    public CodegenExpressionMember addMember(int column, Class type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }
        CodegenExpressionMemberWCol ref = new CodegenExpressionMemberWCol(name, column);
        members.put(ref, type);
        return ref;
    }

    public LinkedHashMap<CodegenExpressionMemberWCol, Class> getMembers() {
        return members;
    }
}
