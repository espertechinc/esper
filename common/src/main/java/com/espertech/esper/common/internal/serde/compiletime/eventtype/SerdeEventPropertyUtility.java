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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.serde.DIOJsonAnyValueSerde;
import com.espertech.esper.common.internal.event.json.serde.DIOJsonArraySerde;
import com.espertech.esper.common.internal.event.json.serde.DIOJsonObjectSerde;
import com.espertech.esper.common.internal.serde.compiletime.resolve.*;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOSkipSerde;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.internal.context.aifactory.createtable.StmtForgeMethodCreateTable.INTERNAL_RESERVED_PROPERTY;
import static com.espertech.esper.common.internal.event.core.EventTypeUtility.resolveTypeCodegenGivenResolver;

public class SerdeEventPropertyUtility {
    public static SerdeEventPropertyDesc forgeForEventProperty(EventType eventTypeSerde, String propertyName, Object propertyType, StatementRawInfo raw, SerdeCompileTimeResolver resolver) {

        DataInputOutputSerdeForge forge;
        if (propertyType == EPTypeNull.INSTANCE) {
            return new SerdeEventPropertyDesc(DataInputOutputSerdeForgeSkip.INSTANCE, Collections.emptySet());
        }
        if (propertyType instanceof EPTypeClass) {
            EPTypeClass epType = (EPTypeClass) propertyType;

            // handle special Json catch-all types
            if (eventTypeSerde instanceof JsonEventType) {
                forge = null;
                if (epType.getType() == Map.class) {
                    forge = new DataInputOutputSerdeForgeSingleton(DIOJsonObjectSerde.class);
                } else if (epType.getType() == Object[].class) {
                    forge = new DataInputOutputSerdeForgeSingleton(DIOJsonArraySerde.class);
                } else if (epType.getType() == Object.class) {
                    forge = new DataInputOutputSerdeForgeSingleton(DIOJsonAnyValueSerde.class);
                }
                if (forge != null) {
                    return new SerdeEventPropertyDesc(forge, Collections.emptySet());
                }
            }

            // handle all Class-type properties
            if (epType.getType() == Object.class && propertyName.equals(INTERNAL_RESERVED_PROPERTY)) {
                forge = new DataInputOutputSerdeForgeSingleton(DIOSkipSerde.class); // for expression data window or others that include transient references in the field
            } else {
                forge = resolver.serdeForEventProperty(epType, eventTypeSerde.getName(), propertyName, raw);
            }
            return new SerdeEventPropertyDesc(forge, Collections.emptySet());
        }

        if (propertyType instanceof EventType) {
            EventType eventType = (EventType) propertyType;
            Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression> func = vars ->
                resolveTypeCodegenGivenResolver(eventType, vars.getOptionalEventTypeResolver());
            forge = new DataInputOutputSerdeForgeEventSerde(DataInputOutputSerdeForgeEventSerdeMethod.NULLABLEEVENT, eventType, func);
            return new SerdeEventPropertyDesc(forge, Collections.singleton(eventType));
        } else if (propertyType instanceof EventType[]) {
            EventType eventType = ((EventType[]) propertyType)[0];
            Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression> func = vars ->
                resolveTypeCodegenGivenResolver(eventType, vars.getOptionalEventTypeResolver());
            forge = new DataInputOutputSerdeForgeEventSerde(DataInputOutputSerdeForgeEventSerdeMethod.NULLABLEEVENTARRAY, eventType, func);
            return new SerdeEventPropertyDesc(forge, Collections.singleton(eventType));
        } else if (propertyType instanceof TypeBeanOrUnderlying) {
            EventType eventType = ((TypeBeanOrUnderlying) propertyType).getEventType();
            Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression> func = vars ->
                resolveTypeCodegenGivenResolver(eventType, vars.getOptionalEventTypeResolver());
            forge = new DataInputOutputSerdeForgeEventSerde(DataInputOutputSerdeForgeEventSerdeMethod.NULLABLEEVENTORUNDERLYING, eventType, func);
            return new SerdeEventPropertyDesc(forge, Collections.singleton(eventType));
        } else if (propertyType instanceof TypeBeanOrUnderlying[]) {
            EventType eventType = ((TypeBeanOrUnderlying[]) propertyType)[0].getEventType();
            Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression> func = vars ->
                resolveTypeCodegenGivenResolver(eventType, vars.getOptionalEventTypeResolver());
            forge = new DataInputOutputSerdeForgeEventSerde(DataInputOutputSerdeForgeEventSerdeMethod.NULLABLEEVENTARRAYORUNDERLYING, eventType, func);
            return new SerdeEventPropertyDesc(forge, Collections.singleton(eventType));
        } else if (propertyType instanceof Map) {
            Map<String, Object> kv = (Map<String, Object>) propertyType;
            String[] keys = new String[kv.size()];
            DataInputOutputSerdeForge[] serdes = new DataInputOutputSerdeForge[kv.size()];
            int index = 0;
            LinkedHashSet<EventType> nestedTypes = new LinkedHashSet<>();
            for (Map.Entry<String, Object> entry : kv.entrySet()) {
                keys[index] = entry.getKey();
                if (entry.getValue() instanceof String) {
                    String value = entry.getValue().toString().trim();
                    Class clazz = JavaClassHelper.getPrimitiveClassForName(value);
                    if (clazz != null) {
                        entry.setValue(clazz);
                    }
                }
                SerdeEventPropertyDesc desc = forgeForEventProperty(eventTypeSerde, entry.getKey(), entry.getValue(), raw, resolver);
                nestedTypes.addAll(desc.getNestedTypes());
                serdes[index] = desc.getForge();
                index++;
            }
            forge = new DataInputOutputSerdeForgeMap(keys, serdes);
            return new SerdeEventPropertyDesc(forge, nestedTypes);
        } else {
            throw new EPException("Failed to determine serde for unrecognized property value type '" + propertyType + "' for property '" + propertyName + "' of type '" + eventTypeSerde.getName() + "'");
        }
    }
}
