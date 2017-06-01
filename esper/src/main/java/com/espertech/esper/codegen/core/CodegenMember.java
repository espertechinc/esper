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

import java.util.Set;

public class CodegenMember {
    private final String memberName;
    private final Class clazz;
    private final Class optionalTypeParam;
    private final Object object;

    protected CodegenMember(String memberName, Class clazz, Object object) {
        this.memberName = memberName;
        this.clazz = clazz;
        this.optionalTypeParam = null;
        this.object = object;
    }

    protected CodegenMember(String memberName, Class clazz, Class optionalTypeParam, Object object) {
        this.memberName = memberName;
        this.clazz = clazz;
        this.optionalTypeParam = optionalTypeParam;
        this.object = object;
    }

    public Class getClazz() {
        return clazz;
    }

    public Class getOptionalTypeParam() {
        return optionalTypeParam;
    }

    public String getMemberName() {
        return memberName;
    }

    public Object getObject() {
        return object;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenMember that = (CodegenMember) o;

        return memberName.equals(that.memberName);
    }

    public int hashCode() {
        return memberName.hashCode();
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(clazz);
        if (optionalTypeParam != null) {
            classes.add(optionalTypeParam);
        }
    }
}
