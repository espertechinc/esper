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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextMetaData {
    private final String contextName;
    private final String contextModuleName;
    private final NameAccessModifier contextVisibility;
    private final EventType eventType;
    private final ContextControllerPortableInfo[] validationInfos;

    public ContextMetaData(String contextName, String contextModuleName, NameAccessModifier contextVisibility, EventType eventType, ContextControllerPortableInfo[] validationInfos) {
        this.contextName = contextName;
        this.contextModuleName = contextModuleName;
        this.contextVisibility = contextVisibility;
        this.eventType = eventType;
        this.validationInfos = validationInfos;
    }

    public EventType getEventType() {
        return eventType;
    }

    public ContextControllerPortableInfo[] getValidationInfos() {
        return validationInfos;
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

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        CodegenExpression[] validationInfos = new CodegenExpression[this.validationInfos.length];
        for (int i = 0; i < validationInfos.length; i++) {
            validationInfos[i] = this.validationInfos[i].make(addInitSvc);
        }
        return newInstance(ContextMetaData.class,
                constant(contextName),
                constant(contextModuleName),
                constant(contextVisibility),
                EventTypeUtility.resolveTypeCodegen(eventType, addInitSvc),
                newArrayWithInit(ContextControllerPortableInfo.class, validationInfos));
    }
}
