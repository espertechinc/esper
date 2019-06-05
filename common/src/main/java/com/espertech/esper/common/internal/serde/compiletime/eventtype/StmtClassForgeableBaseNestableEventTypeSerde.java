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
package com.espertech.esper.common.internal.serde.compiletime.eventtype;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUnderlying;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.serde.DIOJsonObjectSerde;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgeableBaseNestableEventTypeSerde implements StmtClassForgeable {

    private static final String OBJECT_NAME = "obj";
    private static final String OUTPUT_NAME = "output";
    private static final String INPUT_NAME = "input";
    private static final String UNITKEY_NAME = "unitKey";
    private static final String WRITER_NAME = "writer";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final BaseNestableEventType eventType;
    private final DataInputOutputSerdeForge[] forges;

    public StmtClassForgeableBaseNestableEventTypeSerde(String className, CodegenPackageScope packageScope, BaseNestableEventType eventType, DataInputOutputSerdeForge[] forges) {
        this.className = className;
        this.packageScope = packageScope;
        this.eventType = eventType;
        this.forges = forges;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod writeMethod = CodegenMethod.makeParentNode(void.class, StmtClassForgeableBaseNestableEventTypeSerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(Object.class, OBJECT_NAME)
            .addParam(DataOutput.class, OUTPUT_NAME)
            .addParam(byte[].class, UNITKEY_NAME)
            .addParam(EventBeanCollatedWriter.class, WRITER_NAME)
            .addThrown(IOException.class);
        makeWriteMethod(writeMethod);
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);

        CodegenMethod readMethod = CodegenMethod.makeParentNode(Object.class, StmtClassForgeableBaseNestableEventTypeSerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(DataInput.class, INPUT_NAME)
            .addParam(byte[].class, UNITKEY_NAME)
            .addThrown(IOException.class);
        makeReadMethod(readMethod);
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        List<CodegenTypedParam> members = new ArrayList<>();
        for (int i = 0; i < forges.length; i++) {
            members.add(new CodegenTypedParam(forges[i].forgeClassName(), "s" + i));
        }

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(EventTypeResolver.class, "resolver", false));
        CodegenCtor providerCtor = new CodegenCtor(this.getClass(), includeDebugSymbols, ctorParams);
        for (int i = 0; i < forges.length; i++) {
            providerCtor.getBlock().assignRef("s" + i, forges[i].codegen(providerCtor, classScope, ref("resolver")));
        }

        return new CodegenClass(CodegenClassType.EVENTSERDE, DataInputOutputSerde.class, className, classScope, members, providerCtor, methods, Collections.emptyList());
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.MULTIKEY;
    }

    private void makeWriteMethod(CodegenMethod writeMethod) {
        String[] propertyNames = eventType.getPropertyNames();

        if (eventType instanceof MapEventType) {
            writeMethod.getBlock().declareVar(Map.class, "map", cast(Map.class, ref(OBJECT_NAME)));
        } else if (eventType instanceof ObjectArrayEventType) {
            writeMethod.getBlock().declareVar(Object[].class, "oa", cast(Object[].class, ref(OBJECT_NAME)));
        } else if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            writeMethod.getBlock().declareVar(jsonEventType.getUnderlyingType(), "json", cast(jsonEventType.getUnderlyingType(), ref(OBJECT_NAME)));
        } else {
            throw new IllegalStateException("Unrecognized event type " + eventType);
        }

        for (int i = 0; i < forges.length; i++) {
            CodegenExpression serde = ref("s" + i);
            CodegenExpression get;

            if (eventType instanceof MapEventType) {
                get = exprDotMethod(ref("map"), "get", constant(propertyNames[i]));
            } else if (eventType instanceof ObjectArrayEventType) {
                get = arrayAtIndex(ref("oa"), constant(i));
            } else {
                JsonEventType jsonEventType = (JsonEventType) eventType;
                String property = eventType.getPropertyNames()[i];
                JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(property);
                if (field == null) {
                    throw new IllegalStateException("Unrecognized json event property " + property);
                }
                get = ref("json." + field.getFieldName());
            }

            writeMethod.getBlock().exprDotMethod(serde, "write", get, ref(OUTPUT_NAME), ref(UNITKEY_NAME), ref(WRITER_NAME));
        }

        if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            if (jsonEventType.getDetail().isDynamic()) {
                CodegenExpression get = ref("json." + StmtClassForgeableJsonUnderlying.DYNAMIC_PROP_FIELD);
                writeMethod.getBlock().exprDotMethod(publicConstValue(DIOJsonObjectSerde.class, "INSTANCE"), "write", get, ref(OUTPUT_NAME), ref(UNITKEY_NAME), ref(WRITER_NAME));
            }
        }
    }

    private void makeReadMethod(CodegenMethod readMethod) {
        String[] propertyNames = eventType.getPropertyNames();
        CodegenExpressionRef underlyingRef;

        if (eventType instanceof MapEventType) {
            readMethod.getBlock().declareVar(Map.class, "map", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(forges.length))));
            underlyingRef = ref("map");
        } else if (eventType instanceof ObjectArrayEventType) {
            readMethod.getBlock().declareVar(Object[].class, "oa", newArrayByLength(Object.class, constant(forges.length)));
            underlyingRef = ref("oa");
        } else if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            readMethod.getBlock().declareVar(jsonEventType.getUnderlyingType(), "json", newInstance(jsonEventType.getUnderlyingType()));
            underlyingRef = ref("json");
        } else {
            throw new IllegalStateException("Unrecognized event type " + eventType);
        }

        for (int i = 0; i < forges.length; i++) {
            CodegenExpression serde = ref("s" + i);
            CodegenExpression read = exprDotMethod(serde, "read", ref(INPUT_NAME), ref(UNITKEY_NAME));

            if (eventType instanceof MapEventType) {
                readMethod.getBlock().exprDotMethod(ref("map"), "put", constant(propertyNames[i]), read);
            } else if (eventType instanceof ObjectArrayEventType) {
                readMethod.getBlock().assignArrayElement(ref("oa"), constant(i), read);
            } else {
                JsonEventType jsonEventType = (JsonEventType) eventType;
                String property = eventType.getPropertyNames()[i];
                JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(property);
                if (field == null) {
                    throw new IllegalStateException("Unrecognized json event property " + property);
                }
                readMethod.getBlock().assignRef(ref("json." + field.getFieldName()), cast(field.getPropertyType(), read));
            }
        }

        if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            if (jsonEventType.getDetail().isDynamic()) {
                CodegenExpression read = exprDotMethod(publicConstValue(DIOJsonObjectSerde.class, "INSTANCE"), "read", ref(INPUT_NAME), ref(UNITKEY_NAME));
                readMethod.getBlock().assignRef(ref("json." + StmtClassForgeableJsonUnderlying.DYNAMIC_PROP_FIELD), read);
            }
        }

        readMethod.getBlock().methodReturn(underlyingRef);
    }
}
