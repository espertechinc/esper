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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.JsonSchema;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeWithSupertype;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeForgablesPair;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.core.EventTypeNestableGetterFactoryJson;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventTypeDetail;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeNull;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryClassTyped;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryEventTypeTyped;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeNull;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.util.IdentifierUtil.getIdentifierMayStartNumeric;
import static com.espertech.esper.common.internal.event.core.BaseNestableEventUtil.resolvePropertyTypes;

public class JsonEventTypeUtility {
    public static JsonEventType makeJsonTypeCompileTimeExistingType(EventTypeMetadata metadata, JsonEventType existingType, StatementCompileTimeServices services) {
        EventTypeNestableGetterFactoryJson getterFactoryJson = new EventTypeNestableGetterFactoryJson(existingType.getDetail());
        return new JsonEventType(metadata, existingType.getTypes(),
            null, Collections.emptySet(), existingType.getStartTimestampPropertyName(), existingType.getEndTimestampPropertyName(),
            getterFactoryJson, services.getBeanEventTypeFactoryPrivate(), existingType.getDetail(), existingType.getUnderlyingType());
    }

    public static EventTypeForgablesPair makeJsonTypeCompileTimeNewType(EventTypeMetadata metadata, Map<String, Object> compiledTyping, Pair<EventType[], Set<EventType>> superTypes, ConfigurationCommonEventTypeWithSupertype config, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        if (metadata.getApplicationType() != EventTypeApplicationType.JSON) {
            throw new IllegalStateException("Expected Json application type");
        }

        JsonEventType optionalSuperType = (JsonEventType) (superTypes == null ? null : (superTypes.getFirst() == null || superTypes.getFirst().length == 0 ? null : superTypes.getFirst()[0]));
        int numFieldsSuperType = optionalSuperType == null ? 0 : optionalSuperType.getDetail().getFieldDescriptors().size();

        Map<String, Object> properties = resolvePropertyTypes(compiledTyping, services.getEventTypeCompileTimeResolver());
        properties = removeEventBeanTypes(properties);
        Map<String, String> fieldNames = computeFieldNames(properties);
        Map<String, JsonUnderlyingField> fieldDescriptors = computeFields(properties, fieldNames, optionalSuperType);
        Map<String, JsonForgeDesc> forges = computeValueForges(properties, raw.getAnnotations(), services);

        // determine dynamic
        boolean dynamic;
        if (optionalSuperType != null && optionalSuperType.getDetail().isDynamic()) {
            dynamic = true;
        } else {
            JsonSchema jsonSchema = (JsonSchema) AnnotationUtil.findAnnotation(raw.getAnnotations(), JsonSchema.class);
            dynamic = jsonSchema != null && jsonSchema.dynamic();
        }

        String jsonClassName = metadata.getName();
        if (metadata.getAccessModifier().isPrivateOrTransient()) {
            String uuid = CodeGenerationIDGenerator.generateClassNameUUID();
            jsonClassName = jsonClassName + "__" + uuid;
        } else if (raw.getModuleName() != null) {
            jsonClassName = jsonClassName + "__" + "module" + "_" + raw.getModuleName();
        }

        final String underlyingClassName = jsonClassName;
        Map<String, Object> propertySet = properties;
        StmtClassForgeableFactory underlying = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonUnderlying(underlyingClassName, packageScope, propertySet, fieldDescriptors, dynamic, numFieldsSuperType, optionalSuperType, forges);
            }
        };

        final String delegateClassName = jsonClassName + "__Delegate";
        StmtClassForgeableFactory delegate = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonDelegate(delegateClassName, packageScope, propertySet, fieldDescriptors, forges, underlyingClassName, dynamic, optionalSuperType);
            }
        };

        final String delegateFactoryClassName = jsonClassName + "__Factory";
        StmtClassForgeableFactory delegateFactory = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonDelegateFactory(delegateFactoryClassName, packageScope, delegateClassName, underlyingClassName);
            }
        };

        String underlyingClassNameFull = services.getPackageName() + "." + underlyingClassName;
        String delegateClassNameFull = services.getPackageName() + "." + delegateClassName;
        String delegateFactoryClassNameFull = services.getPackageName() + "." + delegateFactoryClassName;
        String serdeClassNameFull = services.getPackageName() + "." + jsonClassName + "__Serde";

        JsonEventTypeDetail detail = new JsonEventTypeDetail(underlyingClassNameFull, delegateClassNameFull, delegateFactoryClassNameFull, serdeClassNameFull, fieldDescriptors, dynamic, numFieldsSuperType);
        EventTypeNestableGetterFactoryJson getterFactoryJson = new EventTypeNestableGetterFactoryJson(detail);

        Class standIn = services.getCompilerServices().compileStandInClass(CodegenClassType.JSONEVENT, underlyingClassName, services.getServices());

        JsonEventType eventType = new JsonEventType(metadata, properties,
            superTypes == null ? new EventType[0] : superTypes.getFirst(),
            superTypes == null ? Collections.emptySet() : superTypes.getSecond(),
            config == null ? null : config.getStartTimestampPropertyName(),
            config == null ? null : config.getEndTimestampPropertyName(),
            getterFactoryJson, services.getBeanEventTypeFactoryPrivate(), detail, standIn);
        return new EventTypeForgablesPair(eventType, Arrays.asList(underlying, delegate, delegateFactory));
    }

    private static Map<String, Object> removeEventBeanTypes(Map<String, Object> properties) {
        LinkedHashMap<String, Object> verified = new LinkedHashMap<>();
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            String propertyName = prop.getKey();
            Object propertyType = prop.getValue();
            verified.put(propertyName, propertyType);

            if (propertyType instanceof EventType) {
                EventType eventType = (EventType) propertyType;
                verified.put(propertyName, new TypeBeanOrUnderlying(eventType));
            } else if (propertyType instanceof EventType[]) {
                EventType eventType = ((EventType[]) propertyType)[0];
                verified.put(propertyName, new TypeBeanOrUnderlying[]{new TypeBeanOrUnderlying(eventType)});
            }
        }
        return verified;
    }

    private static Map<String, JsonForgeDesc> computeValueForges(Map<String, Object> compiledTyping, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        Map<String, JsonForgeDesc> valueForges = new HashMap<>();
        for (Map.Entry<String, Object> entry : compiledTyping.entrySet()) {
            Object type = entry.getValue();
            JsonForgeDesc forgeDesc;
            if (type == null) {
                forgeDesc = new JsonForgeDesc(null, null, JsonEndValueForgeNull.INSTANCE, JsonWriteForgeNull.INSTANCE);
            } else if (type instanceof Class) {
                Class clazz = (Class) type;
                forgeDesc = JsonForgeFactoryClassTyped.forge(clazz, entry.getKey(), annotations, services);
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) type).getEventType();
                validateJsonOrMapType(eventType);
                if (eventType instanceof JsonEventType) {
                    forgeDesc = JsonForgeFactoryEventTypeTyped.forgeNonArray((JsonEventType) eventType);
                } else {
                    forgeDesc = JsonForgeFactoryClassTyped.forge(Map.class, entry.getKey(), annotations, services);
                }
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                EventType eventType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                validateJsonOrMapType(eventType);
                if (eventType instanceof JsonEventType) {
                    forgeDesc = JsonForgeFactoryEventTypeTyped.forgeArray((JsonEventType) eventType);
                } else {
                    forgeDesc = JsonForgeFactoryClassTyped.forge(Map[].class, entry.getKey(), annotations, services);
                }
            } else {
                throw new IllegalStateException("Unrecognized type " + type);
            }
            valueForges.put(entry.getKey(), forgeDesc);
        }
        return valueForges;
    }

    private static void validateJsonOrMapType(EventType eventType) throws ExprValidationException {
        if (!(eventType instanceof JsonEventType) && !(eventType instanceof MapEventType)) {
            throw new ExprValidationException("Failed to validate event type '" + eventType.getMetadata().getName() + "', expected a Json or Map event type");
        }
    }

    private static Map<String, JsonUnderlyingField> computeFields(Map<String, Object> compiledTyping, Map<String, String> fieldNames, JsonEventType optionalSuperType) throws ExprValidationException {
        Map<String, JsonUnderlyingField> allFieldsInclSupertype = new LinkedHashMap<>();

        int index = 0;
        if (optionalSuperType != null) {
            allFieldsInclSupertype.putAll(optionalSuperType.getDetail().getFieldDescriptors());
            index = allFieldsInclSupertype.size();
        }

        for (Map.Entry<String, Object> entry : compiledTyping.entrySet()) {
            String fieldName = fieldNames.get(entry.getKey());

            Object type = entry.getValue();
            Class assignedType;
            if (type == null) {
                assignedType = Object.class;
            } else if (type instanceof Class) {
                assignedType = (Class) type;
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType other = ((TypeBeanOrUnderlying) type).getEventType();
                validateJsonOrMapType(other);
                assignedType = getAssignedType(other);
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                EventType other = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                validateJsonOrMapType(other);
                assignedType = JavaClassHelper.getArrayType(getAssignedType(other));
            } else {
                throw new IllegalStateException("Unrecognized type " + type);
            }

            allFieldsInclSupertype.put(entry.getKey(), new JsonUnderlyingField(fieldName, index, assignedType));
            index++;
        }
        return allFieldsInclSupertype;
    }

    private static Class getAssignedType(EventType type) throws ExprValidationException {
        if (type instanceof JsonEventType) {
            return type.getUnderlyingType();
        }
        if (type instanceof MapEventType) {
            return Map.class;
        }
        throw new ExprValidationException("Incompatible type '" + type.getName() + "' encountered, expected a Json or Map event type");
    }

    private static Map<String, String> computeFieldNames(Map<String, Object> compiledTyping) {
        Map<String, String> fields = new HashMap<>();
        Set<String> assignedNames = new HashSet<>();
        for (String name : compiledTyping.keySet()) {
            String assigned = "_" + getIdentifierMayStartNumeric(name.toLowerCase(Locale.ENGLISH));
            if (!assignedNames.add(assigned)) {
                int suffix = 0;
                while (true) {
                    String withSuffix = assigned + "_" + suffix;
                    if (!assignedNames.contains(withSuffix)) {
                        assigned = withSuffix;
                        assignedNames.add(assigned);
                        break;
                    }
                    suffix++;
                }
            }
            fields.put(name, assigned);
        }
        return fields;
    }

    public static void addJsonUnderlyingClass(Map<String, EventType> moduleTypes, ParentClassLoader classLoaderParent, String optionalDeploymentId) {
        for (Map.Entry<String, EventType> eventType : moduleTypes.entrySet()) {
            addJsonUnderlyingClassInternal(eventType.getValue(), classLoaderParent, optionalDeploymentId);
        }
    }

    public static void addJsonUnderlyingClass(PathRegistry<String, EventType> pathEventTypes, ParentClassLoader classLoaderParent) {
        pathEventTypes.traverse(type -> addJsonUnderlyingClassInternal(type, classLoaderParent, null));
    }

    private static void addJsonUnderlyingClassInternal(EventType eventType, ParentClassLoader classLoaderParent, String optionalDeploymentId) {
        if (!(eventType instanceof JsonEventType)) {
            return;
        }
        JsonEventType jsonEventType = (JsonEventType) eventType;
        // for named-window the same underlying is used and we ignore duplicate add
        boolean allowDuplicate = eventType.getMetadata().getTypeClass() == EventTypeTypeClass.NAMED_WINDOW;
        classLoaderParent.add(jsonEventType.getDetail().getUnderlyingClassName(), jsonEventType.getUnderlyingType(), optionalDeploymentId, allowDuplicate);
        classLoaderParent.add(jsonEventType.getDetail().getDelegateClassName(), jsonEventType.getDelegateType(), optionalDeploymentId, allowDuplicate);
        classLoaderParent.add(jsonEventType.getDetail().getDelegateFactoryClassName(), jsonEventType.getDelegateFactory().getClass(), optionalDeploymentId, allowDuplicate);
    }
}
