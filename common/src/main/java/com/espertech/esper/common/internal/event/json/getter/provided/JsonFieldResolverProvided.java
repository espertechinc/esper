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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EPException;

import java.lang.reflect.Field;

public class JsonFieldResolverProvided {
    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param declaringClass class
     * @param fieldName field name
     * @return field
     */
    public static Field resolveJsonField(Class declaringClass, String fieldName) {
        try {
            return declaringClass.getField(fieldName);
        } catch (Exception ex) {
            throw new EPException("Failed to resolve field '" + fieldName + "' of declaring class '" + declaringClass.getName() + "': " + ex.getMessage(), ex);
        }
    }
}
