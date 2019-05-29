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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateEventObjectArray;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForge;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueRefs;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForge;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeByMethod;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonForgeFactoryEventTypeTyped {
    public static JsonForgeDesc forgeNonArray(JsonEventType other) {
        JsonDelegateForge startObject = new JsonDelegateForge() {
            public CodegenExpression newDelegate(JsonDelegateRefs fields, CodegenMethod parent, CodegenClassScope classScope) {
                CodegenMethod method = parent.makeChild(JsonDelegateBase.class, JsonForgeFactoryEventTypeTyped.class, classScope);
                method.getBlock()
                    .declareVar(JsonDelegateFactory.class, "factory", newInstance(other.getDetail().getDelegateFactoryClassName()))
                    .methodReturn(exprDotMethod(ref("factory"), "make", fields.getBaseHandler(), fields.getThis()));
                return localMethod(method);
            }
        };

        JsonEndValueForge end = new JsonEndValueForge() {
            public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
                return cast(other.getDetail().getUnderlyingClassName(), refs.getValueObject());
            }
        };
        JsonWriteForge writeForge = new JsonWriteForgeByMethod("writeNested");
        return new JsonForgeDesc(startObject, null, end, writeForge);
    }

    public static JsonForgeDesc forgeArray(JsonEventType other) {
        JsonDelegateForge startArray = new JsonDelegateForge() {
            public CodegenExpression newDelegate(JsonDelegateRefs fields, CodegenMethod parent, CodegenClassScope classScope) {
                CodegenMethod method = parent.makeChild(JsonDelegateEventObjectArray.class, JsonForgeFactoryEventTypeTyped.class, classScope);
                method.getBlock()
                    .declareVar(JsonDelegateFactory.class, "factory", newInstance(other.getDetail().getDelegateFactoryClassName()))
                    .methodReturn(newInstance(JsonDelegateEventObjectArray.class, fields.getBaseHandler(), fields.getThis(), ref("factory"), constant(other.getUnderlyingType())));
                return localMethod(method);
            }
        };

        JsonEndValueForge end = new JsonEndValueForge() {
            public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
                return cast(JavaClassHelper.getArrayType(other.getUnderlyingType()), refs.getValueObject());
            }
        };

        JsonWriteForge writeForge = new JsonWriteForgeByMethod("writeNestedArray");
        return new JsonForgeDesc(null, startArray, end, writeForge);
    }
}
