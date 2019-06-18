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
package com.espertech.esper.common.internal.event.json.compiletime;

import java.lang.reflect.Field;
import java.util.List;

public class JsonApplicationClassDelegateDesc {
    private final String delegateClassName;
    private final String delegateFactoryClassName;
    private final List<Field> fields;

    public JsonApplicationClassDelegateDesc(String delegateClassName, String delegateFactoryClassName, List<Field> fields) {
        this.delegateClassName = delegateClassName;
        this.delegateFactoryClassName = delegateFactoryClassName;
        this.fields = fields;
    }

    public String getDelegateClassName() {
        return delegateClassName;
    }

    public String getDelegateFactoryClassName() {
        return delegateFactoryClassName;
    }

    public List<Field> getFields() {
        return fields;
    }
}
