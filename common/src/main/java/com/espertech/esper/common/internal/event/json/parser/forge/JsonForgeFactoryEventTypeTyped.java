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
package com.espertech.esper.common.internal.event.json.parser.forge;

import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForge;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeCast;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForge;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeByMethod;
import com.espertech.esper.common.internal.event.json.write.JsonWriteUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonForgeFactoryEventTypeTyped {
    public static JsonForgeDesc forgeNonArray(String fieldName, JsonEventType other) {
        JsonDelegateForge startObject = new JsonDelegateForgeWithDelegateFactory(other.getDetail().getDelegateFactoryClassName());
        JsonEndValueForge end = new JsonEndValueForgeCast(other.getDetail().getUnderlyingClassName());
        JsonWriteForge writeForge;
        if (other.getDetail().getOptionalUnderlyingProvided() == null) {
            writeForge = new JsonWriteForgeByMethod("writeNested");
        } else {
            writeForge = (refs, method, classScope) -> staticMethod(JsonWriteUtil.class, "writeNested", refs.getWriter(), refs.getField(), newInstance(other.getDetail().getDelegateFactoryClassName()));
        }
        return new JsonForgeDesc(fieldName, startObject, null, end, writeForge);
    }

    public static JsonForgeDesc forgeArray(String fieldName, JsonEventType other) {
        JsonDelegateForge startArray = new JsonDelegateForgeWithDelegateFactoryArray(other.getDetail().getDelegateFactoryClassName(), other.getUnderlyingType());
        JsonEndValueForge end = new JsonEndValueForgeCast(JavaClassHelper.getArrayType(other.getUnderlyingType()));
        JsonWriteForge writeForge;
        if (other.getDetail().getOptionalUnderlyingProvided() == null) {
            writeForge = new JsonWriteForgeByMethod("writeNestedArray");
        } else {
            writeForge = (refs, method, classScope) -> staticMethod(JsonWriteUtil.class, "writeNestedArray", refs.getWriter(), refs.getField(), newInstance(other.getDetail().getDelegateFactoryClassName()));
        }
        return new JsonForgeDesc(fieldName, null, startArray, end, writeForge);
    }
}
