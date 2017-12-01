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
package com.espertech.esper.codegen.base;

import java.util.IdentityHashMap;

public class CodegenClassScope {

    private final boolean debug;
    private final IdentityHashMap<Object, CodegenMember> members = new IdentityHashMap<>();
    private int currentMemberNumber;

    public CodegenClassScope(boolean debug) {
        this.debug = debug;
    }

    public CodegenClassScope(boolean debug, int currentMemberNumber) {
        this.debug = debug;
        this.currentMemberNumber = currentMemberNumber;
    }

    public <T> CodegenMember makeAddMember(Class<? extends T> clazz, T object) {
        CodegenMember existing = members.get(object);
        if (existing != null && object != null) {
            return existing;
        }

        int memberNumber = currentMemberNumber++;
        CodegenMember member = new CodegenMember(new CodegenMemberId(memberNumber), clazz, object);
        members.put(object != null ? object : new Object(), member);
        return member;
    }

    public IdentityHashMap<Object, CodegenMember> getMembers() {
        return members;
    }

    public boolean isDebug() {
        return debug;
    }
}
