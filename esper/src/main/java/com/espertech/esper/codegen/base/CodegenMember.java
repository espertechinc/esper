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

import java.util.Set;

public class CodegenMember {
    private final CodegenMemberId memberId;
    private final Class clazz;
    private final Class optionalTypeParam;
    private final Object object;

    protected CodegenMember(CodegenMemberId memberId, Class clazz, Object object) {
        this.memberId = memberId;
        this.clazz = clazz;
        this.optionalTypeParam = null;
        this.object = object;
    }

    protected CodegenMember(CodegenMemberId memberId, Class clazz, Class optionalTypeParam, Object object) {
        this.memberId = memberId;
        this.clazz = clazz;
        this.optionalTypeParam = optionalTypeParam;
        this.object = object;
    }

    public Class getMemberClass() {
        // returns the actual implementation may safe a virtual call
        return object == null ? clazz : object.getClass();
    }

    public Class getOptionalTypeParam() {
        return optionalTypeParam;
    }

    public CodegenMemberId getMemberId() {
        return memberId;
    }

    public Object getObject() {
        return object;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenMember that = (CodegenMember) o;

        return memberId.equals(that.memberId);
    }

    public int hashCode() {
        return memberId.hashCode();
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(getMemberClass());
        if (optionalTypeParam != null) {
            classes.add(optionalTypeParam);
        }
    }
}
