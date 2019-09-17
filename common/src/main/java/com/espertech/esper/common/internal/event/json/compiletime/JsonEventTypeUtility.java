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
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryBuiltinClassTyped;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeFactoryEventTypeTyped;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeNull;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.ConstructorHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

        // determine supertype
        JsonEventType optionalSuperType = (JsonEventType) (superTypes == null ? null : (superTypes.getFirst() == null || superTypes.getFirst().length == 0 ? null : superTypes.getFirst()[0]));
        int numFieldsSuperType = optionalSuperType == null ? 0 : optionalSuperType.getDetail().getFieldDescriptors().size();

        // determine dynamic
        JsonSchema jsonSchema = (JsonSchema) AnnotationUtil.findAnnotation(raw.getAnnotations(), JsonSchema.class);
        boolean dynamic = determineDynamic(jsonSchema, optionalSuperType, raw);

        // determine json underlying type class
        Class optionalUnderlyingProvided = determineUnderlyingProvided(jsonSchema, services);

        // determine properties
        Map<String, Object> properties;
        Map<String, String> fieldNames;
        Map<Class, JsonApplicationClassDelegateDesc> deepClasses;
        Map<String, Field> fields;
        if (optionalUnderlyingProvided == null) {
            properties = resolvePropertyTypes(compiledTyping, services.getEventTypeCompileTimeResolver());
            properties = removeEventBeanTypes(properties);
            fieldNames = computeFieldNames(properties);
            deepClasses = JsonEventTypeUtilityReflective.computeClassesDeep(properties, metadata.getName(), raw.getAnnotations(), services);
            fields = Collections.emptyMap();
        } else {
            if (dynamic) {
                throw new ExprValidationException("The dynamic flag is not supported when used with a provided JSON event class");
            }
            if (optionalSuperType != null) {
                throw new ExprValidationException("Specifying a supertype is not supported with a provided JSON event class");
            }
            if (!Modifier.isPublic(optionalUnderlyingProvided.getModifiers())) {
                throw new ExprValidationException("Provided JSON event class is not public");
            }
            if (!ConstructorHelper.hasDefaultConstructor(optionalUnderlyingProvided)) {
                throw new ExprValidationException("Provided JSON event class does not have a public default constructor or is a non-static inner class");
            }
            deepClasses = JsonEventTypeUtilityReflective.computeClassesDeep(optionalUnderlyingProvided, metadata.getName(), raw.getAnnotations(), services);
            fields = new LinkedHashMap<>();
            deepClasses.get(optionalUnderlyingProvided).getFields().forEach(field -> fields.put(field.getName(), field));
            properties = resolvePropertiesFromFields(fields);
            fieldNames = computeFieldNamesFromProperties(properties);
            compiledTyping = resolvePropertyTypes(compiledTyping, services.getEventTypeCompileTimeResolver());
            validateFieldTypes(optionalUnderlyingProvided, fields, compiledTyping);

            // use the rich-type definition for properties that may come from events
            for (Map.Entry<String, Object> compiledTypingEntry : compiledTyping.entrySet()) {
                if (compiledTypingEntry.getValue() instanceof TypeBeanOrUnderlying || compiledTypingEntry.getValue() instanceof TypeBeanOrUnderlying[]) {
                    properties.put(compiledTypingEntry.getKey(), compiledTypingEntry.getValue());
                }
            }
        }

        Map<String, JsonUnderlyingField> fieldDescriptors = computeFields(properties, fieldNames, optionalSuperType, fields);
        Map<String, JsonForgeDesc> forges = computeValueForges(properties, fields, deepClasses, raw.getAnnotations(), services);

        String jsonClassNameSimple;
        if (optionalUnderlyingProvided != null) {
            jsonClassNameSimple = optionalUnderlyingProvided.getSimpleName();
        } else {
            jsonClassNameSimple = metadata.getName();
            if (metadata.getAccessModifier().isPrivateOrTransient()) {
                String uuid = CodeGenerationIDGenerator.generateClassNameUUID();
                jsonClassNameSimple = jsonClassNameSimple + "__" + uuid;
            } else if (raw.getModuleName() != null) {
                jsonClassNameSimple = jsonClassNameSimple + "__" + "module" + "_" + raw.getModuleName();
            }
        }

        StmtClassForgeableJsonDesc forgeableDesc = new StmtClassForgeableJsonDesc(properties, fieldDescriptors, dynamic, numFieldsSuperType, optionalSuperType, forges);

        final String underlyingClassNameSimple = jsonClassNameSimple;
        String underlyingClassNameForReference = optionalUnderlyingProvided != null ? optionalUnderlyingProvided.getName() : underlyingClassNameSimple;
        StmtClassForgeableFactory underlying = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonUnderlying(underlyingClassNameSimple, packageScope, forgeableDesc);
            }
        };

        final String delegateClassNameSimple = jsonClassNameSimple + "__Delegate";
        StmtClassForgeableFactory delegate = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonDelegate(CodegenClassType.JSONDELEGATE, delegateClassNameSimple, packageScope, underlyingClassNameForReference, forgeableDesc);
            }
        };

        final String delegateFactoryClassNameSimple = jsonClassNameSimple + "__Factory";
        StmtClassForgeableFactory delegateFactory = new StmtClassForgeableFactory() {
            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableJsonDelegateFactory(CodegenClassType.JSONDELEGATEFACTORY, delegateFactoryClassNameSimple, optionalUnderlyingProvided != null, packageScope, delegateClassNameSimple, underlyingClassNameForReference, forgeableDesc);
            }
        };

        String underlyingClassNameFull = optionalUnderlyingProvided == null ? services.getPackageName() + "." + underlyingClassNameSimple : optionalUnderlyingProvided.getName();
        String delegateClassNameFull = services.getPackageName() + "." + delegateClassNameSimple;
        String delegateFactoryClassNameFull = services.getPackageName() + "." + delegateFactoryClassNameSimple;
        String serdeClassNameFull = services.getPackageName() + "." + jsonClassNameSimple + "__" + metadata.getName() + "__Serde"; // include event type name as underlying-class may occur multiple times

        JsonEventTypeDetail detail = new JsonEventTypeDetail(underlyingClassNameFull, optionalUnderlyingProvided, delegateClassNameFull, delegateFactoryClassNameFull, serdeClassNameFull, fieldDescriptors, dynamic, numFieldsSuperType);
        EventTypeNestableGetterFactoryJson getterFactoryJson = new EventTypeNestableGetterFactoryJson(detail);

        Class standIn = optionalUnderlyingProvided == null ? services.getCompilerServices().compileStandInClass(CodegenClassType.JSONEVENT, underlyingClassNameSimple, services.getServices())
            : optionalUnderlyingProvided;

        JsonEventType eventType = new JsonEventType(metadata, properties,
            superTypes == null ? new EventType[0] : superTypes.getFirst(),
            superTypes == null ? Collections.emptySet() : superTypes.getSecond(),
            config == null ? null : config.getStartTimestampPropertyName(),
            config == null ? null : config.getEndTimestampPropertyName(),
            getterFactoryJson, services.getBeanEventTypeFactoryPrivate(), detail, standIn);

        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(3);

        // generate delegate and factory forgables for application classes
        generateApplicationClassForgables(optionalUnderlyingProvided, deepClasses, additionalForgeables, raw.getAnnotations(), services);

        if (optionalUnderlyingProvided == null) {
            additionalForgeables.add(underlying);
        }
        additionalForgeables.add(delegate);
        additionalForgeables.add(delegateFactory);

        return new EventTypeForgablesPair(eventType, additionalForgeables);
    }

    private static void validateFieldTypes(Class declaredClass, Map<String, Field> targetFields, Map<String, Object> insertedFields) throws ExprValidationException {
        for (Map.Entry<String, Object> inserted : insertedFields.entrySet()) {
            String insertedName = inserted.getKey();
            Object insertedType = inserted.getValue();
            Field field = targetFields.get(insertedName);

            if (field == null) {
                throw new ExprValidationException("Failed to find public field '" + insertedName + "' on class '" + declaredClass.getName() + "'");
            }

            Class fieldClass = JavaClassHelper.getBoxedType(field.getType());
            if (insertedType instanceof Class) {
                Class insertedClass = JavaClassHelper.getBoxedType((Class) insertedType);
                if (!JavaClassHelper.isSubclassOrImplementsInterface(insertedClass, fieldClass)) {
                    throw makeInvalidField(insertedName, insertedClass, declaredClass, field);
                }
            } else if (insertedType instanceof TypeBeanOrUnderlying || insertedType instanceof EventType) {
                EventType eventType = (insertedType instanceof TypeBeanOrUnderlying) ? ((TypeBeanOrUnderlying) insertedType).getEventType() : (EventType) insertedType;
                if (!JavaClassHelper.isSubclassOrImplementsInterface(eventType.getUnderlyingType(), fieldClass)) {
                    throw makeInvalidField(insertedName, eventType.getUnderlyingType(), declaredClass, field);
                }
            } else if (insertedType instanceof TypeBeanOrUnderlying[] || insertedType instanceof EventType[]) {
                EventType eventType = (insertedType instanceof TypeBeanOrUnderlying[]) ? ((TypeBeanOrUnderlying[]) insertedType)[0].getEventType() : ((EventType[]) insertedType)[0];
                if (!fieldClass.isArray() || !JavaClassHelper.isSubclassOrImplementsInterface(eventType.getUnderlyingType(), fieldClass.getComponentType())) {
                    throw makeInvalidField(insertedName, eventType.getUnderlyingType(), declaredClass, field);
                }
            } else {
                throw new IllegalStateException("Unrecognized type '" + insertedType + "'");
            }
        }
    }

    private static ExprValidationException makeInvalidField(String insertedName, Class insertedClass, Class declaredClass, Field field) {
        return new ExprValidationException("Public field '" + insertedName + "' of class '" + JavaClassHelper.getClassNameFullyQualPretty(declaredClass) + "' declared as type " +
            "'" + JavaClassHelper.getClassNameFullyQualPretty(field.getType()) + "' cannot receive a value of type '" + JavaClassHelper.getClassNameFullyQualPretty(insertedClass) + "'");
    }

    private static void generateApplicationClassForgables(Class optionalUnderlyingProvided, Map<Class, JsonApplicationClassDelegateDesc> deepClasses, List<StmtClassForgeableFactory> additionalForgeables, Annotation[] annotations, StatementCompileTimeServices services)
        throws ExprValidationException {
        for (Map.Entry<Class, JsonApplicationClassDelegateDesc> entry : deepClasses.entrySet()) {
            if (entry.getKey() == optionalUnderlyingProvided) {
                continue;
            }

            LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
            entry.getValue().getFields().forEach(field -> fields.put(field.getName(), field));

            Map<String, Object> properties = resolvePropertiesFromFields(fields);
            Map<String, String> fieldNames = computeFieldNamesFromProperties(properties);
            Map<String, JsonForgeDesc> forges = computeValueForges(properties, fields, deepClasses, annotations, services);
            Map<String, JsonUnderlyingField> fieldDescriptors = computeFields(properties, fieldNames, null, fields);

            final String delegateClassNameSimple = entry.getValue().getDelegateClassName();
            StmtClassForgeableJsonDesc forgeableDesc = new StmtClassForgeableJsonDesc(properties, fieldDescriptors, false, 0, null, forges);
            StmtClassForgeableFactory delegate = new StmtClassForgeableFactory() {
                public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                    return new StmtClassForgeableJsonDelegate(CodegenClassType.JSONNESTEDCLASSDELEGATEANDFACTORY, delegateClassNameSimple, packageScope, entry.getKey().getName(), forgeableDesc);
                }
            };

            final String delegateFactoryClassNameSimple = entry.getValue().getDelegateFactoryClassName();
            StmtClassForgeableFactory delegateFactory = new StmtClassForgeableFactory() {
                public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                    return new StmtClassForgeableJsonDelegateFactory(CodegenClassType.JSONNESTEDCLASSDELEGATEANDFACTORY, delegateFactoryClassNameSimple, true, packageScope, delegateClassNameSimple, entry.getKey().getName(), forgeableDesc);
                }
            };

            additionalForgeables.add(delegate);
            additionalForgeables.add(delegateFactory);
        }
    }

    private static Map<String, String> computeFieldNamesFromProperties(Map<String, Object> properties) {
        HashMap<String, String> fieldNames = new LinkedHashMap<>();
        for (String key : properties.keySet()) {
            fieldNames.put(key, key);
        }
        return fieldNames;
    }

    private static Class determineUnderlyingProvided(JsonSchema jsonSchema, StatementCompileTimeServices services)
        throws ExprValidationException {
        if (jsonSchema != null && !jsonSchema.className().trim().isEmpty()) {
            try {
                return services.getClasspathImportServiceCompileTime().resolveClass(jsonSchema.className(), true);
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve JSON event class '" + jsonSchema.className() + "': " + e.getMessage(), e);
            }
        }
        return null;
    }

    private static boolean determineDynamic(JsonSchema jsonSchema, JsonEventType optionalSuperType, StatementRawInfo raw) {
        if (optionalSuperType != null && optionalSuperType.getDetail().isDynamic()) {
            return true;
        }
        return jsonSchema != null && jsonSchema.dynamic();
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

    private static Map<String, JsonForgeDesc> computeValueForges(Map<String, Object> compiledTyping, Map<String, Field> fields, Map<Class, JsonApplicationClassDelegateDesc> deepClasses, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        Map<String, JsonForgeDesc> valueForges = new HashMap<>();
        for (Map.Entry<String, Object> entry : compiledTyping.entrySet()) {
            Object type = entry.getValue();
            Field optionalField = fields.get(entry.getKey());
            JsonForgeDesc forgeDesc;
            if (type == null) {
                forgeDesc = new JsonForgeDesc(entry.getKey(), null, null, JsonEndValueForgeNull.INSTANCE, JsonWriteForgeNull.INSTANCE);
            } else if (type instanceof Class) {
                Class clazz = (Class) type;
                forgeDesc = JsonForgeFactoryBuiltinClassTyped.forge(clazz, entry.getKey(), optionalField, deepClasses, annotations, services);
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) type).getEventType();
                validateJsonOrMapType(eventType);
                if (eventType instanceof JsonEventType) {
                    forgeDesc = JsonForgeFactoryEventTypeTyped.forgeNonArray(entry.getKey(), (JsonEventType) eventType);
                } else {
                    forgeDesc = JsonForgeFactoryBuiltinClassTyped.forge(Map.class, entry.getKey(), optionalField, deepClasses, annotations, services);
                }
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                EventType eventType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                validateJsonOrMapType(eventType);
                if (eventType instanceof JsonEventType) {
                    forgeDesc = JsonForgeFactoryEventTypeTyped.forgeArray(entry.getKey(), (JsonEventType) eventType);
                } else {
                    forgeDesc = JsonForgeFactoryBuiltinClassTyped.forge(Map[].class, entry.getKey(), optionalField, deepClasses, annotations, services);
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

    private static Map<String, JsonUnderlyingField> computeFields(Map<String, Object> compiledTyping, Map<String, String> fieldNames, JsonEventType optionalSuperType, Map<String, Field> fields) throws ExprValidationException {
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

            allFieldsInclSupertype.put(entry.getKey(), new JsonUnderlyingField(fieldName, index, assignedType, fields.get(fieldName)));
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
        if (jsonEventType.getDetail().getOptionalUnderlyingProvided() == null) {
            classLoaderParent.add(jsonEventType.getDetail().getUnderlyingClassName(), jsonEventType.getUnderlyingType(), optionalDeploymentId, allowDuplicate);
        } else {
            allowDuplicate = true;
        }
        classLoaderParent.add(jsonEventType.getDetail().getDelegateClassName(), jsonEventType.getDelegateType(), optionalDeploymentId, allowDuplicate);
        classLoaderParent.add(jsonEventType.getDetail().getDelegateFactoryClassName(), jsonEventType.getDelegateFactory().getClass(), optionalDeploymentId, allowDuplicate);
    }

    private static Map<String, Object> resolvePropertiesFromFields(Map<String, Field> fields) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>(CollectionUtil.capacityHashMap(fields.size()));
        for (Map.Entry<String, Field> field : fields.entrySet()) {
            properties.put(field.getKey(), field.getValue().getType());
        }
        return properties;
    }
}
