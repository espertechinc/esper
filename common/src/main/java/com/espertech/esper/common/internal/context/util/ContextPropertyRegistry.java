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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerKeyedValidation;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerKeyedValidationItem;

import java.util.Locale;

public class ContextPropertyRegistry {

    public final static String CONTEXT_PREFIX = "context";

    private final EventType contextEventType;
    private final ContextControllerPortableInfo[] controllerValidations;

    public ContextPropertyRegistry(ContextMetaData metaData) {
        this.contextEventType = metaData.getEventType();
        this.controllerValidations = metaData.getValidationInfos();
    }

    public boolean isPartitionProperty(EventType fromType, String propertyName) {
        String name = getPartitionContextPropertyName(fromType, propertyName);
        return name != null;
    }

    public String getPartitionContextPropertyName(EventType fromType, String propertyName) {
        if (controllerValidations.length == 1) {
            if (controllerValidations[0] instanceof ContextControllerKeyedValidation) {
                ContextControllerKeyedValidation partitioned = (ContextControllerKeyedValidation) controllerValidations[0];
                for (ContextControllerKeyedValidationItem item : partitioned.getItems()) {
                    if (item.getEventType() == fromType) {
                        for (int i = 0; i < item.getPropertyNames().length; i++) {
                            if (item.getPropertyNames()[i].equals(propertyName)) {
                                return ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isContextPropertyPrefix(String prefixName) {
        return prefixName != null && prefixName.toLowerCase(Locale.ENGLISH).equals(CONTEXT_PREFIX);
    }

    public EventType getContextEventType() {
        return contextEventType;
    }
}
