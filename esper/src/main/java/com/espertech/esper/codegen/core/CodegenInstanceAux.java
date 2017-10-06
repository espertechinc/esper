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
package com.espertech.esper.codegen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodegenInstanceAux {
    private final CodegenCtor serviceCtor;
    private final CodegenNamedMethods methods = new CodegenNamedMethods();
    private List<CodegenTypedParam> members;

    public CodegenInstanceAux(CodegenCtor serviceCtor) {
        this.serviceCtor = serviceCtor;
    }

    public CodegenCtor getServiceCtor() {
        return serviceCtor;
    }

    public void addMember(String name, Class type) {
        if (members == null) {
            members = new ArrayList<>(2);
        }
        for (CodegenTypedParam member : members) {
            if (member.getName().equals(name)) {
                throw new IllegalStateException("Member by name '" + name + "' already added");
            }
        }
        members.add(new CodegenTypedParam(type, name));
    }

    public boolean hasMember(String name) {
        if (members == null) {
            return false;
        }
        for (CodegenTypedParam member : members) {
            if (member.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public List<CodegenTypedParam> getMembers() {
        return members == null ? Collections.emptyList() : members;
    }

    public CodegenNamedMethods getMethods() {
        return methods;
    }
}
