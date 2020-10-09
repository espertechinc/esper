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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class CodegenField {
    private final String clazz;
    private final String name;
    private final EPTypeClass type;
    private final boolean isFinal;
    private String assignmentMemberName;

    public CodegenField(String clazz, String name, EPTypeClass type, boolean isFinal) {
        this.clazz = clazz;
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenField that = (CodegenField) o;

        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public void mergeClasses(Set<Class> classes) {
        type.traverseClasses(classes::add);
    }

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public EPTypeClass getType() {
        return type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void render(StringBuilder builder) {
        builder.append(clazz).append('.');
        if (assignmentMemberName != null) {
            builder.append(assignmentMemberName).append(".");
        }
        builder.append(name);
    }

    public void setAssignmentMemberName(String assignmentMemberName) {
        this.assignmentMemberName = assignmentMemberName;
    }

    public CodegenExpressionRef getNameWithMember() {
        if (assignmentMemberName == null) {
            return ref(name);
        }
        return ref(assignmentMemberName + "." + name);
    }
}
