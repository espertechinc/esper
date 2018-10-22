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
package com.espertech.esper.common.internal.context.compile;

import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class ContextCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, ContextMetaData> contexts = new HashMap<>();

    public void newContext(ContextMetaData detail) {
        if (!detail.getContextVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for contexts");
        }
        String name = detail.getContextName();
        ContextMetaData existing = contexts.get(name);
        if (existing != null) {
            throw new IllegalStateException("A duplicate definition of contexts was detected for name '" + name + "'");
        }
        contexts.put(name, detail);
    }

    public Map<String, ContextMetaData> getContexts() {
        return contexts;
    }
}
