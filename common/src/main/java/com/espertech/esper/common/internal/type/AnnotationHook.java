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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.annotation.Hook;
import com.espertech.esper.common.client.annotation.HookType;

import java.lang.annotation.Annotation;

public class AnnotationHook implements Hook {
    private final HookType type;
    private final String hook;

    public AnnotationHook(HookType type, String hook) {
        this.type = type;
        this.hook = hook;
    }

    public String hook() {
        return hook;
    }

    public HookType type() {
        return type;
    }

    public Class<? extends Annotation> annotationType() {
        return Hook.class;
    }
}
