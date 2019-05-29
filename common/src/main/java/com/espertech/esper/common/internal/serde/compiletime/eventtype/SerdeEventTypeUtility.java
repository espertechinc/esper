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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator.generateClassNameUUID;
import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator.generateClassNameWithUUID;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventPropertyUtility.forgeForEventProperty;

public class SerdeEventTypeUtility {
    public static List<StmtClassForgeableFactory> plan(EventType eventType, StatementRawInfo raw, SerdeEventTypeCompileTimeRegistry registry, SerdeCompileTimeResolver resolver) {
        if (!registry.isTargetHA() || registry.getEventTypes().containsKey(eventType) || eventType.getMetadata().getTypeClass() == EventTypeTypeClass.TABLE_INTERNAL) {
            return Collections.emptyList();
        }
        List<StmtClassForgeableFactory> forgeables = new ArrayList<>(2);
        planRecursive(forgeables, eventType, raw, registry, resolver);
        return forgeables;
    }

    private static void planRecursive(List<StmtClassForgeableFactory> additionalForgeables, EventType eventType, StatementRawInfo raw, SerdeEventTypeCompileTimeRegistry registry, SerdeCompileTimeResolver resolver) {
        if (!registry.isTargetHA()) {
            return;
        }
        if (registry.getEventTypes().containsKey(eventType)) {
            return;
        }

        SerdeAndForgeables pair;
        if (eventType instanceof BeanEventType) {
            pair = planBean((BeanEventType) eventType, raw, resolver);
        } else if (eventType instanceof BaseNestableEventType) {
            pair = planBaseNestable((BaseNestableEventType) eventType, raw, resolver);
            planPropertiesMayRecurse(eventType, additionalForgeables, raw, registry, resolver);
        } else if (eventType instanceof WrapperEventType) {
            WrapperEventType wrapperEventType = (WrapperEventType) eventType;
            planRecursive(additionalForgeables, wrapperEventType.getUnderlyingEventType(), raw, registry, resolver);
            pair = planBaseNestable(wrapperEventType.getUnderlyingMapType(), raw, resolver);
        } else if (eventType instanceof VariantEventType || eventType instanceof AvroSchemaEventType || eventType instanceof BaseXMLEventType) {
            // no serde generation
            pair = null;
        } else {
            throw new UnsupportedOperationException("Event type not yet handled: " + eventType);
        }

        if (pair != null) {
            registry.addSerdeFor(eventType, pair.forge);
            additionalForgeables.addAll(pair.additionalForgeables);
        }
    }

    private static void planPropertiesMayRecurse(EventType eventType, List<StmtClassForgeableFactory> additionalForgeables, StatementRawInfo raw, SerdeEventTypeCompileTimeRegistry registry, SerdeCompileTimeResolver resolver) {
        for (EventPropertyDescriptor desc : eventType.getPropertyDescriptors()) {
            if (!desc.isFragment()) {
                continue;
            }
            FragmentEventType fragmentEventType = eventType.getFragmentType(desc.getPropertyName());
            if (fragmentEventType == null || registry.getEventTypes().containsKey(fragmentEventType.getFragmentType())) {
                continue;
            }

            planRecursive(additionalForgeables, fragmentEventType.getFragmentType(), raw, registry, resolver);
        }
    }

    private static SerdeAndForgeables planBaseNestable(BaseNestableEventType eventType, StatementRawInfo raw, SerdeCompileTimeResolver resolver) {
        String className;
        if (eventType instanceof JsonEventType) {
            String classNameFull = ((JsonEventType) eventType).getDetail().getSerdeClassName();
            int lastDotIndex = classNameFull.lastIndexOf('.');
            className = lastDotIndex == -1 ? classNameFull : classNameFull.substring(lastDotIndex + 1);

        } else {
            String uuid = generateClassNameUUID();
            className = generateClassNameWithUUID(DataInputOutputSerde.class, eventType.getMetadata().getName(), uuid);
        }

        DataInputOutputSerdeForge optionalApplicationSerde = resolver.serdeForEventTypeExternalProvider(eventType, raw);
        if (optionalApplicationSerde != null) {
            return new SerdeAndForgeables(optionalApplicationSerde, Collections.emptyList());
        }

        DataInputOutputSerdeForge[] forges = new DataInputOutputSerdeForge[eventType.getTypes().size()];
        int count = 0;
        for (Map.Entry<String, Object> property : eventType.getTypes().entrySet()) {
            SerdeEventPropertyDesc desc = forgeForEventProperty(eventType, property.getKey(), property.getValue(), raw, resolver);
            forges[count] = desc.getForge();
            count++;
        }

        StmtClassForgeableFactory forgeable = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableBaseNestableEventTypeSerde(className, packageScope, eventType, forges);
            }
        };

        DataInputOutputSerdeForge forge = new DataInputOutputSerdeForge() {
            public String forgeClassName() {
                return className;
            }

            public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
                return newInstance(className, optionalEventTypeResolver);
            }
        };

        return new SerdeAndForgeables(forge, Collections.singletonList(forgeable));
    }

    private static SerdeAndForgeables planBean(BeanEventType eventType, StatementRawInfo raw, SerdeCompileTimeResolver resolver) {
        DataInputOutputSerdeForge forge = resolver.serdeForBeanEventType(raw, eventType.getUnderlyingType(), eventType.getName(), eventType.getSuperTypes());
        return new SerdeAndForgeables(forge, Collections.emptyList());
    }

    private static class SerdeAndForgeables {
        private final DataInputOutputSerdeForge forge;
        private final List<StmtClassForgeableFactory> additionalForgeables;

        public SerdeAndForgeables(DataInputOutputSerdeForge forge, List<StmtClassForgeableFactory> additionalForgeables) {
            this.forge = forge;
            this.additionalForgeables = additionalForgeables;
        }

        public DataInputOutputSerdeForge getForge() {
            return forge;
        }

        public List<StmtClassForgeableFactory> getAdditionalForgeables() {
            return additionalForgeables;
        }
    }
}
