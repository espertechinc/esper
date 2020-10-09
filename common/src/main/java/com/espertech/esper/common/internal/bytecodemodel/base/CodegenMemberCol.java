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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMemberWCol;

import java.util.*;

public class CodegenMemberCol {
    private final LinkedHashMap<CodegenExpressionMemberWCol, EPTypeClass> members = new LinkedHashMap<>();

    public CodegenExpressionMember addMember(int column, EPTypeClass type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }
        CodegenExpressionMemberWCol ref = new CodegenExpressionMemberWCol(name, column);
        members.put(ref, type);
        return ref;
    }

    public void put(CodegenExpressionMemberWCol member, EPTypeClass type) {
        members.put(member, type);
    }

    public LinkedHashMap<CodegenExpressionMemberWCol, EPTypeClass> getMembers() {
        return members;
    }

    public TreeMap<Integer, List<CodegenExpressionMemberWCol>> getMembersPerColumn() {
        TreeMap<Integer, List<CodegenExpressionMemberWCol>> columns = new TreeMap<>();
        for (Map.Entry<CodegenExpressionMemberWCol, EPTypeClass> entry : members.entrySet()) {
            int col = entry.getKey().getCol();
            List<CodegenExpressionMemberWCol> members = columns.computeIfAbsent(col, k -> new ArrayList<>(2));
            members.add(entry.getKey());
        }
        return columns;
    }

    public EPTypeClass get(CodegenExpressionMemberWCol member) {
        return members.get(member);
    }
}
