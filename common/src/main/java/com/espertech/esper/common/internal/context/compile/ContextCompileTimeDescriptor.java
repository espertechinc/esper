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

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;

public class ContextCompileTimeDescriptor {
    private final String contextName;
    private final String contextModuleName;
    private final NameAccessModifier contextVisibility;
    private final ContextPropertyRegistry contextPropertyRegistry;
    private final ContextControllerPortableInfo[] validationInfos;

    public ContextCompileTimeDescriptor(String contextName, String contextModuleName, NameAccessModifier contextVisibility, ContextPropertyRegistry contextPropertyRegistry, ContextControllerPortableInfo[] validationInfos) {
        this.contextName = contextName;
        this.contextModuleName = contextModuleName;
        this.contextVisibility = contextVisibility;
        this.contextPropertyRegistry = contextPropertyRegistry;
        this.validationInfos = validationInfos;
    }

    public String getContextName() {
        return contextName;
    }

    public String getContextModuleName() {
        return contextModuleName;
    }

    public NameAccessModifier getContextVisibility() {
        return contextVisibility;
    }

    public ContextPropertyRegistry getContextPropertyRegistry() {
        return contextPropertyRegistry;
    }

    public ContextControllerPortableInfo[] getValidationInfos() {
        return validationInfos;
    }
}
