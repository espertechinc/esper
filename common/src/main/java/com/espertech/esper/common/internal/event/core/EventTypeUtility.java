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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.annotation.XMLSchemaField;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.*;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.ColumnDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateSchemaDesc;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEventEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.manufacturer.*;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventBean;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.event.property.*;
import com.espertech.esper.common.internal.event.xml.*;
import com.espertech.esper.common.internal.settings.*;
import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.common.internal.util.*;
import org.w3c.dom.Node;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EventTypeUtility {

    /**
     * Compare tyoes
     * @param newEventType the proposed new type
     * @param existingType the existing type
     * @throws ExprValidationException when validation fails
     */
    public static void compareExistingType(EventType newEventType, EventType existingType) throws ExprValidationException {
        ExprValidationException compared = ((EventTypeSPI) newEventType).equalsCompareType(existingType);
        if (compared != null) {
            throw new ExprValidationException("Event type named '" + newEventType.getName() +
                "' has already been declared with differing column name or type information: " + compared.getMessage(), compared);
        }
    }

    public static EventBeanSPI getShellForType(EventType eventType) {
        if (eventType instanceof BeanEventType) {
            return new BeanEventBean(null, eventType);
        }
        if (eventType instanceof ObjectArrayEventType) {
            return new ObjectArrayEventBean(null, eventType);
        }
        if (eventType instanceof MapEventType) {
            return new MapEventBean(null, eventType);
        }
        if (eventType instanceof BaseXMLEventType) {
            return new XMLEventBean(null, eventType);
        }
        if (eventType instanceof JsonEventType) {
            return new JsonEventBean(null, eventType);
        }
        if (eventType instanceof WrapperEventType) {
            return new WrapperEventBean(null, null, eventType);
        }
        throw new EventAdapterException("Event type '" + eventType.getName() + "' is not an runtime-native event type");
    }

    public static EventBeanFactory getFactoryForType(EventType type, EventBeanTypedEventFactory factory, EventTypeAvroHandler eventTypeAvroHandler) {
        if (type instanceof WrapperEventType) {
            WrapperEventType wrapperType = (WrapperEventType) type;
            if (wrapperType.getUnderlyingEventType() instanceof BeanEventType) {
                return new EventBeanFactoryBeanWrapped(wrapperType.getUnderlyingEventType(), wrapperType, factory);
            }
        }
        if (type instanceof BeanEventType) {
            return new EventBeanFactoryBean(type, factory);
        }
        if (type instanceof MapEventType) {
            return new EventBeanFactoryMap(type, factory);
        }
        if (type instanceof ObjectArrayEventType) {
            return new EventBeanFactoryObjectArray(type, factory);
        }
        if (type instanceof BaseXMLEventType) {
            return new EventBeanFactoryXML(type, factory);
        }
        if (type instanceof AvroSchemaEventType) {
            return eventTypeAvroHandler.getEventBeanFactory(type, factory);
        }
        if (type instanceof JsonEventType) {
            return new EventBeanFactoryJson((JsonEventType) type, factory);
        }
        throw new IllegalArgumentException("Cannot create event bean factory for event type '" + type.getName() + "': " + type.getClass().getName() + " is not a recognized event type or supported wrap event type");
    }

    /**
     * Returns a factory for creating and populating event object instances for the given type.
     *
     * @param eventType              to create underlying objects for
     * @param properties             to write
     * @param classpathImportService for resolving methods
     * @param allowAnyType           whether any type property can be populated
     * @param avroHandler            avro handler
     * @return factory
     * @throws EventBeanManufactureException if a factory cannot be created for the type
     */
    public static EventBeanManufacturerForge getManufacturer(EventType eventType, WriteablePropertyDescriptor[] properties, ClasspathImportService classpathImportService, boolean allowAnyType, EventTypeAvroHandler avroHandler)
        throws EventBeanManufactureException {
        if (!(eventType instanceof EventTypeSPI)) {
            return null;
        }
        if (eventType instanceof BeanEventType) {
            BeanEventType beanEventType = (BeanEventType) eventType;
            return new EventBeanManufacturerBeanForge(beanEventType, properties, classpathImportService);
        }
        EventTypeSPI typeSPI = (EventTypeSPI) eventType;
        if (!allowAnyType && !allowPopulate(typeSPI)) {
            return null;
        }
        if (eventType instanceof MapEventType) {
            MapEventType mapEventType = (MapEventType) eventType;
            return new EventBeanManufacturerMapForge(mapEventType, properties);
        }
        if (eventType instanceof ObjectArrayEventType) {
            ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) eventType;
            return new EventBeanManufacturerObjectArrayForge(objectArrayEventType, properties);
        }
        if (eventType instanceof AvroSchemaEventType) {
            AvroSchemaEventType avroSchemaEventType = (AvroSchemaEventType) eventType;
            return avroHandler.getEventBeanManufacturer(avroSchemaEventType, properties);
        }
        if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            if (jsonEventType.getDetail().getOptionalUnderlyingProvided() != null) {
                return new EventBeanManufacturerJsonProvidedForge(jsonEventType, properties, classpathImportService);
            }
            return new EventBeanManufacturerJsonForge(jsonEventType, properties);
        }
        return null;
    }

    /**
     * Returns descriptors for all writable properties.
     *
     * @param eventType         to reflect on
     * @param allowAnyType      whether any type property can be populated
     * @param allowFragmentType whether to return writeable properties that are typed as event type
     * @return list of writable properties
     */
    public static Set<WriteablePropertyDescriptor> getWriteableProperties(EventType eventType, boolean allowAnyType, boolean allowFragmentType) {
        if (!(eventType instanceof EventTypeSPI)) {
            return null;
        }
        if (eventType instanceof BeanEventType) {
            BeanEventType beanEventType = (BeanEventType) eventType;
            return PropertyHelper.getWritableProperties(beanEventType.getUnderlyingType());
        }
        EventTypeSPI typeSPI = (EventTypeSPI) eventType;
        if (!allowAnyType && !allowPopulate(typeSPI)) {
            return null;
        }
        if (eventType instanceof BaseNestableEventType) {
            Map<String, Object> mapdef = ((BaseNestableEventType) eventType).getTypes();
            Set<WriteablePropertyDescriptor> writables = new LinkedHashSet<>();
            for (Map.Entry<String, Object> types : mapdef.entrySet()) {
                if (types.getValue() instanceof EPTypeClass) {
                    writables.add(new WriteablePropertyDescriptor(types.getKey(), (EPTypeClass) types.getValue(), null, false));
                }
                if (types.getValue() instanceof String) {
                    String typeName = types.getValue().toString();
                    Class clazz = JavaClassHelper.getPrimitiveClassForName(typeName);
                    if (clazz != null) {
                        writables.add(new WriteablePropertyDescriptor(types.getKey(), ClassHelperGenericType.getClassEPType(clazz), null, false));
                    } else if (allowFragmentType) {
                        writables.add(new WriteablePropertyDescriptor(types.getKey(), null, null, true));
                    }
                }
                if (allowFragmentType && types.getValue() instanceof TypeBeanOrUnderlying) {
                    TypeBeanOrUnderlying und = (TypeBeanOrUnderlying) types.getValue();
                    writables.add(new WriteablePropertyDescriptor(types.getKey(), und.getEventType().getUnderlyingEPType(), null, true));
                }
                if (allowFragmentType && types.getValue() instanceof TypeBeanOrUnderlying[]) {
                    TypeBeanOrUnderlying[] und = (TypeBeanOrUnderlying[]) types.getValue();
                    writables.add(new WriteablePropertyDescriptor(types.getKey(), und[0].getEventType().getUnderlyingEPType(), null, true));
                }
            }
            return writables;
        } else if (eventType instanceof AvroSchemaEventType) {
            Set<WriteablePropertyDescriptor> writables = new LinkedHashSet<WriteablePropertyDescriptor>();
            EventPropertyDescriptor[] desc = typeSPI.getWriteableProperties();
            for (EventPropertyDescriptor prop : desc) {
                writables.add(new WriteablePropertyDescriptor(prop.getPropertyName(), prop.getPropertyEPType(), null, false));
            }
            return writables;
        } else {
            return null;
        }
    }

    public static CodegenExpression resolveTypeCodegen(EventType eventType, CodegenExpression initServicesRef) {
        return resolveTypeCodegenGivenResolver(eventType, exprDotMethodChain(initServicesRef).add(EPStatementInitServices.GETEVENTTYPERESOLVER));
    }

    public static CodegenExpression resolveTypeCodegenGivenResolver(EventType eventType, CodegenExpression typeResolver) {
        if (eventType == null) {
            throw new IllegalArgumentException("Null event type");
        }
        if (typeResolver == null) {
            throw new IllegalArgumentException("Event type resolver not provided");
        }
        if (eventType instanceof BeanEventType && eventType.getMetadata().getAccessModifier() == NameAccessModifier.TRANSIENT) {
            BeanEventType beanEventType = (BeanEventType) eventType;
            boolean publicFields = beanEventType.getStem().isPublicFields();
            return exprDotMethod(typeResolver, EventTypeResolver.RESOLVE_PRIVATE_BEAN_METHOD, constant(eventType.getUnderlyingEPType()), constant(publicFields));
        }
        return exprDotMethod(typeResolver, EventTypeResolver.RESOLVE_METHOD, constant(eventType.getMetadata().getName()), constant(eventType.getMetadata().getModuleName()), constant(eventType.getMetadata().getAccessModifier()));
    }

    public static CodegenExpression resolveTypeArrayCodegen(EventType[] eventTypes, CodegenExpression initServicesRef) {
        CodegenExpression[] expressions = new CodegenExpression[eventTypes.length];
        for (int i = 0; i < eventTypes.length; i++) {
            expressions[i] = resolveTypeCodegen(eventTypes[i], initServicesRef);
        }
        return newArrayWithInit(EventType.EPTYPE, expressions);
    }

    public static CodegenExpression resolveTypeArrayCodegenMayNull(EventType[] eventTypes, CodegenExpression initServicesRef) {
        CodegenExpression[] expressions = new CodegenExpression[eventTypes.length];
        for (int i = 0; i < eventTypes.length; i++) {
            expressions[i] = eventTypes[i] == null ? constantNull() : resolveTypeCodegen(eventTypes[i], initServicesRef);
        }
        return newArrayWithInit(EventType.EPTYPE, expressions);
    }

    public static CodegenExpression codegenGetterWCoerce(EventPropertyGetterSPI getter, EPType getterType, EPType optionalCoercionType, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        getterType = JavaClassHelper.getBoxedType(getterType);
        CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), EventPropertyValueGetter.EPTYPE);
        CodegenMethod get = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), generator, classScope).addParam(CodegenNamedParam.from(EventBean.EPTYPE, "bean"));
        anonymous.addMethod("get", get);

        CodegenExpression result = getter.eventBeanGetCodegen(ref("bean"), method, classScope);
        if (optionalCoercionType instanceof EPTypeClass && !getterType.equals(optionalCoercionType) && JavaClassHelper.isNumeric(getterType)) {
            SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(getterType, JavaClassHelper.getBoxedType((EPTypeClass) optionalCoercionType));
            get.getBlock().declareVar((EPTypeClass) getterType, "prop", cast((EPTypeClass) getterType, result));
            result = coercer.coerceCodegen(ref("prop"), (EPTypeClass) getterType);
        }

        get.getBlock().methodReturn(result);
        return anonymous;
    }

    public static CodegenExpression codegenGetterWCoerceWArray(EPTypeClass interfaceClass, EventPropertyGetterSPI getter, EPType getterTypeUnboxed, EPType optionalCoercionType, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        if (getterTypeUnboxed == EPTypeNull.INSTANCE || optionalCoercionType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass getterType = (EPTypeClass) JavaClassHelper.getBoxedType(getterTypeUnboxed);
        CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), interfaceClass);

        List<CodegenNamedParam> params;
        if (interfaceClass.getType() == EventPropertyValueGetter.class) {
            params = CodegenNamedParam.from(EventBean.EPTYPE, "bean");
        } else if (interfaceClass.getType() == ExprEventEvaluator.class) {
            params = CodegenNamedParam.from(EventBean.EPTYPE, "bean", ExprEvaluatorContext.EPTYPE, "ctx");
        } else {
            throw new IllegalStateException("Unrecognized interface class " + interfaceClass);
        }
        CodegenMethod getOrEval = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), generator, classScope).addParam(params);
        anonymous.addMethod(interfaceClass.getType() == EventPropertyValueGetter.class ? "get" : "eval", getOrEval);

        CodegenExpression result = getter.eventBeanGetCodegen(ref("bean"), method, classScope);
        if (optionalCoercionType != null && getterType != optionalCoercionType && JavaClassHelper.isNumeric(getterType)) {
            SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(getterType, JavaClassHelper.getBoxedType((EPTypeClass) optionalCoercionType));
            getOrEval.getBlock().declareVar(getterType, "prop", cast(getterType, result));
            result = coercer.coerceCodegen(ref("prop"), getterType);
        }

        if (getterType.getType().isArray()) {
            EPTypeClass mkType = MultiKeyPlanner.getMKClassForComponentType(JavaClassHelper.getArrayComponentType(getterType));
            result = newInstance(mkType, cast(getterType, result));
        }

        getOrEval.getBlock().methodReturn(result);
        return anonymous;
    }

    public static CodegenExpression codegenWriter(EventType eventType, EPType evaluationType, EventPropertyWriterSPI writer, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), EventPropertyWriter.EPTYPE);
        CodegenMethod write = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), generator, classScope).addParam(CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "value", EventBean.EPTYPE, "bean"));
        anonymous.addMethod("write", write);

        evaluationType = JavaClassHelper.getBoxedType(evaluationType);

        write.getBlock()
            .declareVar(eventType.getUnderlyingEPType(), "und", cast(eventType.getUnderlyingEPType(), exprDotUnderlying(ref("bean"))));
        CodegenExpression value = constantNull();
        if (evaluationType != null && evaluationType != EPTypeNull.INSTANCE) {
            EPTypeClass evalClass = (EPTypeClass) evaluationType;
            write.getBlock().declareVar(evalClass, "eval", cast(evalClass, ref("value")));
            value = ref("eval");
        }
        write.getBlock().expression(writer.writeCodegen(value, ref("und"), ref("bean"), write, classScope));
        return anonymous;
    }

    public static LinkedHashMap<String, Object> getPropertyTypesNonPrimitive(LinkedHashMap<String, Object> propertyTypesMayPrimitive) {
        boolean hasPrimitive = false;
        for (Map.Entry<String, Object> entry : propertyTypesMayPrimitive.entrySet()) {
            if (entry.getValue() instanceof EPTypeClass && ((EPTypeClass) entry.getValue()).getType().isPrimitive()) {
                hasPrimitive = true;
                break;
            }
        }

        if (!hasPrimitive) {
            return propertyTypesMayPrimitive;
        }

        LinkedHashMap<String, Object> props = new LinkedHashMap<>(propertyTypesMayPrimitive);
        for (Map.Entry<String, Object> entry : propertyTypesMayPrimitive.entrySet()) {
            if (entry.getValue() instanceof EPTypeClass) {
                EPTypeClass clazz = (EPTypeClass) entry.getValue();
                if (clazz.getType().isPrimitive()) {
                    props.put(entry.getKey(), JavaClassHelper.getBoxedType(clazz));
                }
            }
        }
        return props;
    }

    public static EventType requireEventType(String aspectCamel, String aspectName, String optionalEventTypeName, EventTypeCompileTimeResolver eventTypeCompileTimeResolver) throws ExprValidationException {
        if (optionalEventTypeName == null) {
            throw new ExprValidationException(aspectCamel + " '" + aspectName + "' returns EventBean-array but does not provide the event type name");
        }
        EventType eventType = eventTypeCompileTimeResolver.getTypeByName(optionalEventTypeName);
        if (eventType == null) {
            throw new ExprValidationException(aspectCamel + " '" + aspectName + "' returns event type '" + optionalEventTypeName + "' and the event type cannot be found");
        }
        return eventType;
    }

    public static Pair<EventType[], Set<EventType>> getSuperTypesDepthFirst(Set<String> superTypesSet, EventUnderlyingType representation, EventTypeNameResolver eventTypeNameResolver) {
        return getSuperTypesDepthFirst(superTypesSet == null || superTypesSet.isEmpty() ? null : superTypesSet.toArray(new String[superTypesSet.size()]), representation, eventTypeNameResolver);
    }

    public static Pair<EventType[], Set<EventType>> getSuperTypesDepthFirst(String[] superTypesSet, EventUnderlyingType representation, EventTypeNameResolver eventTypeNameResolver)
        throws EventAdapterException {
        if (superTypesSet == null || superTypesSet.length == 0) {
            return new Pair<>(null, null);
        }

        EventType[] superTypes = new EventType[superTypesSet.length];
        Set<EventType> deepSuperTypes = new LinkedHashSet<>();

        int count = 0;
        for (String superName : superTypesSet) {
            EventType type = eventTypeNameResolver.getTypeByName(superName);
            if (type == null) {
                throw new EventAdapterException("Supertype by name '" + superName + "' could not be found");
            }
            if (representation == EventUnderlyingType.MAP) {
                if (!(type instanceof MapEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not a Map, expected a Map event type as a supertype");
                }
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                if (!(type instanceof ObjectArrayEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not an Object-array type, expected a Object-array event type as a supertype");
                }
            } else if (representation == EventUnderlyingType.AVRO) {
                if (!(type instanceof AvroSchemaEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not an Avro type, expected a Avro event type as a supertype");
                }
            } else if (representation == EventUnderlyingType.JSON) {
                if (!(type instanceof JsonEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not a Json type, expected a Json event type as a supertype");
                }
            } else {
                throw new IllegalStateException("Unrecognized enum " + representation);
            }
            superTypes[count++] = type;
            deepSuperTypes.add(type);
            addRecursiveSupertypes(deepSuperTypes, type);
        }

        List<EventType> superTypesListDepthFirst = new ArrayList<>(deepSuperTypes);
        Collections.reverse(superTypesListDepthFirst);

        return new Pair<EventType[], Set<EventType>>(superTypes, new LinkedHashSet<>(superTypesListDepthFirst));
    }

    public static EventPropertyDescriptor getNestablePropertyDescriptor(EventType target, String propertyName) {
        EventPropertyDescriptor descriptor = target.getPropertyDescriptor(propertyName);
        if (descriptor != null) {
            return descriptor;
        }
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            return null;
        }
        // parse, can be an nested property
        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof PropertyBase) {
            return target.getPropertyDescriptor(((PropertyBase) property).getPropertyNameAtomic());
        }
        if (!(property instanceof NestedProperty)) {
            return null;
        }
        NestedProperty nested = (NestedProperty) property;
        Deque<Property> properties = new ArrayDeque<Property>(nested.getProperties());
        return getNestablePropertyDescriptor(target, properties);
    }

    public static EventPropertyDescriptor getNestablePropertyDescriptor(EventType target, Deque<Property> stack) {

        Property topProperty = stack.removeFirst();
        if (stack.isEmpty()) {
            return target.getPropertyDescriptor(((PropertyBase) topProperty).getPropertyNameAtomic());
        }

        if (!(topProperty instanceof SimpleProperty)) {
            return null;
        }
        SimpleProperty simple = (SimpleProperty) topProperty;

        FragmentEventType fragmentEventType = target.getFragmentType(simple.getPropertyNameAtomic());
        if (fragmentEventType == null || fragmentEventType.getFragmentType() == null) {
            return null;
        }
        return getNestablePropertyDescriptor(fragmentEventType.getFragmentType(), stack);
    }

    public static LinkedHashMap<String, Object> buildType(List<ColumnDesc> columns, Set<String> copyFrom, ClasspathImportServiceCompileTime classpathImportService, ClasspathExtensionClass classpathExtension, EventTypeNameResolver eventTypeResolver) throws ExprValidationException {
        LinkedHashMap<String, Object> typing = new LinkedHashMap<String, Object>();
        Set<String> columnNames = new HashSet<String>();

        // Copy-from information gets added first as object-array appends information in well-defined order
        if (copyFrom != null && !copyFrom.isEmpty()) {
            for (String copyFromName : copyFrom) {
                EventType type = eventTypeResolver.getTypeByName(copyFromName);
                if (type == null) {
                    throw new ExprValidationException("Type by name '" + copyFromName + "' could not be located");
                }
                mergeType(typing, type, columnNames);
            }
        }

        for (ColumnDesc column : columns) {
            boolean added = columnNames.add(column.getName());
            if (!added) {
                throw new ExprValidationException("Duplicate column name '" + column.getName() + "'");
            }
            Object columnType = buildType(column, classpathImportService, classpathExtension);
            typing.put(column.getName(), columnType);
        }

        return typing;
    }

    public static Object buildType(ColumnDesc column, ClasspathImportServiceCompileTime classpathImportService, ClasspathExtensionClass classpathExtension) throws ExprValidationException {

        if (column.getType() == null) {
            return null;
        }

        ClassDescriptor classIdent = ClassDescriptor.parseTypeText(column.getType());
        EPTypeClass type = ClasspathImportEPTypeUtil.resolveClassIdentifierToEPType(classIdent, false, classpathImportService, ClasspathExtensionClassEmpty.INSTANCE);
        if (type != null) {
            return type;
        }

        // Event types fall into here
        if (classIdent.getArrayDimensions() > 1) {
            throw new EPException("Two-dimensional arrays are not supported for event-types (cannot find class '" + classIdent.getClassIdentifier() + "')");
        }
        if (classIdent.getArrayDimensions() == 1) {
            return column.getType() + "[]";
        }
        return column.getType();
    }

    private static void mergeType(Map<String, Object> typing, EventType typeToMerge, Set<String> columnNames)
        throws ExprValidationException {
        for (EventPropertyDescriptor prop : typeToMerge.getPropertyDescriptors()) {

            Object existing = typing.get(prop.getPropertyName());

            if (!prop.isFragment()) {
                Class assigned = prop.getPropertyType();
                if (existing != null && existing instanceof Class) {
                    if (JavaClassHelper.getBoxedType((Class) existing) != JavaClassHelper.getBoxedType(assigned)) {
                        throw new ExprValidationException("Type by name '" + typeToMerge.getName() + "' contributes property '" +
                            prop.getPropertyName() + "' defined as '" + ClassHelperPrint.getClassNameFullyQualPretty(assigned) +
                            "' which overides the same property of type '" + ClassHelperPrint.getClassNameFullyQualPretty((Class) existing) + "'");
                    }
                }
                typing.put(prop.getPropertyName(), prop.getPropertyType());
            } else {
                if (existing != null) {
                    throw new ExprValidationException("Property by name '" + prop.getPropertyName() + "' is defined twice by adding type '" + typeToMerge.getName() + "'");
                }
                FragmentEventType fragment = typeToMerge.getFragmentType(prop.getPropertyName());
                if (fragment == null) {
                    continue;
                }
                // for native-type fragments (classes) we use the original type as available from map or object-array
                if (typeToMerge instanceof BaseNestableEventType && fragment.isNative()) {
                    BaseNestableEventType baseNestable = (BaseNestableEventType) typeToMerge;
                    typing.put(prop.getPropertyName(), baseNestable.getTypes().get(prop.getPropertyName()));
                } else {
                    if (fragment.isIndexed()) {
                        typing.put(prop.getPropertyName(), new EventType[]{fragment.getFragmentType()});
                    } else {
                        typing.put(prop.getPropertyName(), fragment.getFragmentType());
                    }
                }
            }

            columnNames.add(prop.getPropertyName());
        }
    }

    public static void validateTimestampProperties(EventType eventType, String startTimestampProperty, String endTimestampProperty)
        throws ConfigurationException {

        if (startTimestampProperty != null) {
            if (eventType.getGetter(startTimestampProperty) == null) {
                throw new ConfigurationException("Declared start timestamp property name '" + startTimestampProperty + "' was not found");
            }
            EPType type = eventType.getPropertyEPType(startTimestampProperty);
            if (type == null || type == EPTypeNull.INSTANCE || !JavaClassHelper.isDatetimeClass(type)) {
                throw new ConfigurationException("Declared start timestamp property '" + startTimestampProperty + "' is expected to return a Date, Calendar or long-typed value but returns '" + type + "'");
            }
        }

        if (endTimestampProperty != null) {
            if (startTimestampProperty == null) {
                throw new ConfigurationException("Declared end timestamp property requires that a start timestamp property is also declared");
            }
            if (eventType.getGetter(endTimestampProperty) == null) {
                throw new ConfigurationException("Declared end timestamp property name '" + endTimestampProperty + "' was not found");
            }
            Class type = eventType.getPropertyType(endTimestampProperty);
            if (!JavaClassHelper.isDatetimeClass(type)) {
                throw new ConfigurationException("Declared end timestamp property '" + endTimestampProperty + "' is expected to return a Date, Calendar or long-typed value but returns '" + type.getName() + "'");
            }
            Class startType = eventType.getPropertyType(startTimestampProperty);
            if (JavaClassHelper.getBoxedType(startType) != JavaClassHelper.getBoxedType(type)) {
                throw new ConfigurationException("Declared end timestamp property '" + endTimestampProperty + "' is expected to have the same property type as the start-timestamp property '" + startTimestampProperty + "'");
            }
        }
    }

    public static boolean isTypeOrSubTypeOf(EventType candidate, EventType superType) {
        if (candidate == superType) {
            return true;
        }
        if (candidate.getSuperTypes() != null) {
            for (Iterator<EventType> it = candidate.getDeepSuperTypes(); it.hasNext(); ) {
                if (it.next() == superType) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine among the Map-type properties which properties are Bean-type event type names,
     * rewrites these as Class-type instead so that they are configured as native property and do not require wrapping,
     * but may require unwrapping.
     *
     * @param typing            properties of map type
     * @param eventTypeResolver resolver
     * @return compiled properties, same as original unless Bean-type event type names were specified.
     */
    public static LinkedHashMap<String, Object> compileMapTypeProperties(Map<String, Object> typing, EventTypeNameResolver eventTypeResolver) {
        LinkedHashMap<String, Object> compiled = new LinkedHashMap<String, Object>(typing);
        for (Map.Entry<String, Object> specifiedEntry : typing.entrySet()) {
            Object typeSpec = specifiedEntry.getValue();
            String nameSpec = specifiedEntry.getKey();

            if (typeSpec == null) {
                compiled.put(nameSpec, EPTypeNull.INSTANCE);
                continue;
            }

            if (typeSpec instanceof EPType) {
                compiled.put(nameSpec, JavaClassHelper.getBoxedType((EPType) typeSpec));
                continue;
            }

            if (typeSpec instanceof Class) {
                EPTypeClass epTypeClass = ClassHelperGenericType.getClassEPType(JavaClassHelper.getBoxedType((Class) typeSpec));
                compiled.put(nameSpec, epTypeClass);
                continue;
            }

            if (typeSpec instanceof Map) {
                LinkedHashMap<String, Object> inner = compileMapTypeProperties((Map<String, Object>) typeSpec, eventTypeResolver);
                compiled.put(nameSpec, inner);
            }

            if (!(typeSpec instanceof String)) {
                continue;
            }

            String typeNameSpec = (String) typeSpec;
            boolean isArray = EventTypeUtility.isPropertyArray(typeNameSpec);
            if (isArray) {
                typeNameSpec = EventTypeUtility.getPropertyRemoveArray(typeNameSpec);
            }

            EventType eventType = eventTypeResolver.getTypeByName(typeNameSpec);
            if (!(eventType instanceof BeanEventType)) {
                continue;
            }

            BeanEventType beanEventType = (BeanEventType) eventType;
            EPTypeClass underlyingType = beanEventType.getUnderlyingEPType();
            if (isArray) {
                underlyingType = JavaClassHelper.getArrayType(underlyingType);
            }
            compiled.put(nameSpec, underlyingType);
        }
        return compiled;
    }

    /**
     * Returns true if the name indicates that the type is an array type.
     *
     * @param name the property name
     * @return true if array type
     */
    public static boolean isPropertyArray(String name) {
        return name.trim().endsWith("[]");
    }

    public static boolean isTypeOrSubTypeOf(String typeName, EventType sameTypeOrSubtype) {
        if (sameTypeOrSubtype.getName().equals(typeName)) {
            return true;
        }
        if (sameTypeOrSubtype.getSuperTypes() == null) {
            return false;
        }
        for (EventType superType : sameTypeOrSubtype.getSuperTypes()) {
            if (superType.getName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the property name without the array type extension, if present.
     *
     * @param name property name
     * @return property name with removed array extension name
     */
    public static String getPropertyRemoveArray(String name) {
        return name.replaceAll("\\[", "").replaceAll("\\]", "");
    }

    public static PropertySetDescriptor getNestableProperties(Map<String, Object> propertiesToAdd, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTypeNestableGetterFactory factory, EventType[] optionalSuperTypes, BeanEventTypeFactory beanEventTypeFactory, boolean publicFields)
        throws EPException {
        List<String> propertyNameList = new ArrayList<String>();
        List<EventPropertyDescriptor> propertyDescriptors = new ArrayList<EventPropertyDescriptor>();
        Map<String, Object> nestableTypes = new LinkedHashMap<String, Object>();
        Map<String, PropertySetDescriptorItem> propertyItems = new HashMap<String, PropertySetDescriptorItem>();

        // handle super-types first, such that the order of properties is well-defined from super-type to subtype
        if (optionalSuperTypes != null) {
            for (int i = 0; i < optionalSuperTypes.length; i++) {
                BaseNestableEventType superType = (BaseNestableEventType) optionalSuperTypes[i];
                for (String propertyName : superType.getPropertyNames()) {
                    if (nestableTypes.containsKey(propertyName)) {
                        continue;
                    }
                    propertyNameList.add(propertyName);
                }
                for (EventPropertyDescriptor descriptor : superType.getPropertyDescriptors()) {
                    if (nestableTypes.containsKey(descriptor.getPropertyName())) {
                        continue;
                    }
                    propertyDescriptors.add(descriptor);
                }

                propertyItems.putAll(superType.propertyItems);
                nestableTypes.putAll(superType.nestableTypes);
            }
        }

        nestableTypes.putAll(propertiesToAdd);

        // Initialize getters and names array: at this time we do not care about nested types,
        // these are handled at the time someone is asking for them
        for (Map.Entry<String, Object> entry : propertiesToAdd.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new EPException("Invalid type configuration: property name is not a String-type value");
            }
            String name = entry.getKey();

            // handle types that are String values
            if (entry.getValue() instanceof String) {
                String value = entry.getValue().toString().trim();
                Class clazz = JavaClassHelper.getPrimitiveClassForName(value);
                if (clazz != null) {
                    entry.setValue(clazz);
                }
            }

            if (entry.getValue() instanceof EPTypeClass) {
                EPTypeClass eptype = (EPTypeClass) entry.getValue();

                boolean isIndexed = eptype.getType().isArray() || JavaClassHelper.isImplementsInterface(eptype, Iterable.class);
                boolean isMapped = JavaClassHelper.isImplementsInterface(eptype, Map.class);
                boolean isFragment = JavaClassHelper.isFragmentableType(eptype);
                BeanEventType nativeFragmentType = null;
                FragmentEventType fragmentType = null;
                if (isFragment) {
                    fragmentType = EventBeanUtility.createNativeFragmentType(eptype, beanEventTypeFactory, publicFields);
                    if (fragmentType != null) {
                        nativeFragmentType = (BeanEventType) fragmentType.getFragmentType();
                    } else {
                        isFragment = false;
                    }
                }
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, nativeFragmentType, eventBeanTypedEventFactory, isFragment);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, eptype, false, false, isIndexed, isMapped, isFragment);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, fragmentType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            // A null-type is also allowed
            if (entry.getValue() == EPTypeNull.INSTANCE || entry.getValue() == null) {
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, null, null, false);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, EPTypeNull.INSTANCE, false, false, false, false, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, null);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            // Add Map itself as a property
            if (entry.getValue() instanceof Map) {
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, null, null, false);
                EPType epType = EPTypePremade.MAP.getEPType();
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, epType, false, false, false, true, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, null);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof EventType) {
                // Add EventType itself as a property
                EventType eventType = (EventType) entry.getValue();
                EventPropertyGetterSPI getter = factory.getGetterEventBean(name, eventType.getUnderlyingEPType());
                EPTypeClass epType = eventType.getUnderlyingEPType();
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, epType, false, false, false, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, false, false, true);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof EventType[]) {
                // Add EventType array itself as a property, type is expected to be first array element
                EventType eventType = ((EventType[]) entry.getValue())[0];
                EPTypeClass underlyingType = eventType.getUnderlyingEPType();
                EPTypeClass arrayType = JavaClassHelper.getArrayType(underlyingType);
                EventPropertyGetterSPI getter = factory.getGetterEventBeanArray(name, eventType);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, arrayType, false, false, true, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, true, false, true);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) entry.getValue()).getEventType();
                if (!(eventType instanceof BaseNestableEventType)) {
                    throw new EPException("Nestable type configuration encountered an unexpected property type name '"
                        + entry.getValue() + "' for property '" + name + "', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");
                }

                EPTypeClass underlyingType = eventType.getUnderlyingEPType();
                EventPropertyGetterSPI getter = factory.getGetterBeanNested(name, eventType, eventBeanTypedEventFactory);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, underlyingType, false, false, false, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, false, false, true);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof TypeBeanOrUnderlying[]) {
                EventType eventType = ((TypeBeanOrUnderlying[]) entry.getValue())[0].getEventType();
                if (!(eventType instanceof BaseNestableEventType) && !(eventType instanceof BeanEventType)) {
                    throw new EPException("Nestable type configuration encountered an unexpected property type name '"
                        + entry.getValue() + "' for property '" + name + "', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");
                }

                EPTypeClass underlyingType = eventType.getUnderlyingEPType();
                if (underlyingType.getType() != Object[].class) {
                    underlyingType = JavaClassHelper.getArrayType(underlyingType);
                }
                EventPropertyGetterSPI getter = factory.getGetterBeanNestedArray(name, eventType, eventBeanTypedEventFactory);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, underlyingType, false, false, true, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, true, false, true);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            generateExceptionNestedProp(name, entry.getValue());
        }

        return new PropertySetDescriptor(propertyNameList, propertyDescriptors, propertyItems, nestableTypes);
    }

    private static void generateExceptionNestedProp(String name, Object value) throws EPException {
        String clazzName = (value == null) ? "null" : value.getClass().getSimpleName();
        throw new EPException("Nestable type configuration encountered an unexpected property type of '"
            + clazzName + "' for property '" + name + "', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");
    }

    public static EPType getNestablePropertyType(String propertyName,
                                                 Map<String, PropertySetDescriptorItem> simplePropertyTypes,
                                                 Map<String, Object> nestableTypes,
                                                 BeanEventTypeFactory beanEventTypeFactory,
                                                 boolean publicFields) {
        PropertySetDescriptorItem item = simplePropertyTypes.get(StringValue.unescapeDot(propertyName));
        if (item != null) {
            return item.getPropertyDescriptor().getPropertyEPType();
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            // dynamic simple property
            if (propertyName.endsWith("?")) {
                return EPTypePremade.OBJECT.getEPType();
            }

            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);

            if (property instanceof SimpleProperty) {
                PropertySetDescriptorItem propitem = simplePropertyTypes.get(property.getPropertyNameAtomic());
                if (propitem != null) {
                    return propitem.getPropertyDescriptor().getPropertyEPType();
                }
                return null;
            }

            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                } else if (type instanceof EventType[]) {
                    EventType eventType = ((EventType[]) type)[0];
                    return eventType.getUnderlyingEPType();
                } else if (type instanceof TypeBeanOrUnderlying[]) {
                    EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    return innerType.getUnderlyingEPType();
                } else if (type instanceof EPTypeClass) {
                    EPTypeClass typeClass = (EPTypeClass) type;
                    if (!typeClass.getType().isArray()) {
                        return null;
                    }
                    return JavaClassHelper.getArrayComponentType(typeClass);
                } else { // remove this
                    return null;
                }
            } else if (property instanceof MappedProperty) {
                MappedProperty mappedProp = (MappedProperty) property;
                Object type = nestableTypes.get(mappedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof EPTypeClass) {
                    if (JavaClassHelper.isImplementsInterface((EPTypeClass) type, Map.class)) {
                        return EPTypePremade.OBJECT.getEPType();
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        // Map event types allow 2 types of properties inside:
        //   - a property that is a Java object is interrogated via bean property getters and BeanEventType
        //   - a property that is a Map itself is interrogated via map property getters
        // The property getters therefore act on

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = PropertyParser.unescapeBacktickForProperty(StringValue.unescapeDot(propertyName.substring(0, index)));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());
        boolean isRootedDynamic = false;

        // If the property is dynamic, remove the ? since the property type is defined without
        if (propertyMap.endsWith("?")) {
            propertyMap = propertyMap.substring(0, propertyMap.length() - 1);
            isRootedDynamic = true;
        }

        Object nestedType = nestableTypes.get(propertyMap);
        if (nestedType == null) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyMap);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                // handle map-in-map case
                if (type instanceof TypeBeanOrUnderlying[]) {
                    EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    return innerType.getPropertyEPType(propertyNested);
                } else if (type instanceof EventType[]) {
                    // handle eventtype[] in map
                    EventType innerType = ((EventType[]) type)[0];
                    return innerType.getPropertyEPType(propertyNested);
                } else if (type instanceof EPTypeClass) {
                    if (!((EPTypeClass) type).getType().isArray()) {
                        return null;
                    }
                    EPTypeClass componentType = JavaClassHelper.getArrayComponentType((EPTypeClass) type);
                    BeanEventType beanEventType = beanEventTypeFactory.getCreateBeanType(componentType, publicFields);
                    return beanEventType.getPropertyEPType(propertyNested);
                } else {
                    return null;
                }
            } else if (property instanceof MappedProperty) {
                return null;    // Since no type information is available for the property
            } else {
                return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : null;
            }
        }

        // If there is a map value in the map, return the Object value if this is a dynamic property
        if (nestedType instanceof EPTypeClass && ((EPTypeClass) nestedType).getType() == Map.class) {
            Property prop = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : JavaClassHelper.getBoxedType(prop.getPropertyTypeMap(null, beanEventTypeFactory));   // we don't have a definition of the nested props
        } else if (nestedType instanceof Map) {
            Property prop = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            Map nestedTypes = (Map) nestedType;
            return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : JavaClassHelper.getBoxedType(prop.getPropertyTypeMap(nestedTypes, beanEventTypeFactory));
        } else if (nestedType instanceof EPTypeClass) {
            EPTypeClass simpleClass = (EPTypeClass) nestedType;
            if (JavaClassHelper.isJavaBuiltinDataType(simpleClass)) {
                return null;
            }
            if (simpleClass.getType().isArray() &&
                (JavaClassHelper.isJavaBuiltinDataType(simpleClass.getType().getComponentType()) || simpleClass.getType().getComponentType() == Object.class)) {
                return null;
            }
            EventType nestedEventType = beanEventTypeFactory.getCreateBeanType(simpleClass, publicFields);
            return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : JavaClassHelper.getBoxedType(nestedEventType.getPropertyEPType(propertyNested));
        } else if (nestedType instanceof EventType) {
            EventType innerType = (EventType) nestedType;
            return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : JavaClassHelper.getBoxedType(innerType.getPropertyEPType(propertyNested));
        } else if (nestedType instanceof EventType[]) {
            return null;    // requires indexed property
        } else if (nestedType instanceof TypeBeanOrUnderlying) {
            EventType innerType = ((TypeBeanOrUnderlying) nestedType).getEventType();
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            return isRootedDynamic ? EPTypePremade.OBJECT.getEPType() : JavaClassHelper.getBoxedType(innerType.getPropertyEPType(propertyNested));
        } else if (nestedType instanceof TypeBeanOrUnderlying[]) {
            return null;
        } else {
            String message = "Nestable map type configuration encountered an unexpected value type of '"
                + nestedType.getClass() + "' for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
            throw new PropertyAccessException(message);
        }
    }

    public static EventPropertyGetterSPI getNestableGetter(String propertyName,
                                                           Map<String, PropertySetDescriptorItem> propertyGetters,
                                                           Map<String, EventPropertyGetterSPI> propertyGetterCache,
                                                           Map<String, Object> nestableTypes,
                                                           EventBeanTypedEventFactory eventBeanTypedEventFactory,
                                                           EventTypeNestableGetterFactory factory,
                                                           boolean isObjectArray,
                                                           BeanEventTypeFactory beanEventTypeFactory,
                                                           boolean publicFields) {
        EventPropertyGetterSPI cachedGetter = propertyGetterCache.get(propertyName);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        String unescapePropName = StringValue.unescapeDot(propertyName);
        PropertySetDescriptorItem item = propertyGetters.get(unescapePropName);
        if (item != null) {
            EventPropertyGetterSPI getter = item.getPropertyGetter();
            propertyGetterCache.put(propertyName, getter);
            return getter;
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (prop instanceof SimpleProperty) { // perhaps escaped
                item = propertyGetters.get(prop.getPropertyNameAtomic());
                if (item != null) {
                    EventPropertyGetterSPI getter = item.getPropertyGetter();
                    propertyGetterCache.put(propertyName, getter);
                    return getter;
                }
                return null;
            } else if (prop instanceof DynamicProperty) {
                DynamicProperty dynamicProperty = (DynamicProperty) prop;
                EventPropertyGetterSPI getterDyn = factory.getPropertyDynamicGetter(nestableTypes, propertyName, dynamicProperty, eventBeanTypedEventFactory, beanEventTypeFactory);
                propertyGetterCache.put(propertyName, getterDyn);
                return getterDyn;
            } else if (prop instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) prop;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                } else if (type instanceof EventType[]) {
                    EventPropertyGetterSPI getterArr = factory.getGetterIndexedEventBean(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex());
                    propertyGetterCache.put(propertyName, getterArr);
                    return getterArr;
                } else if (type instanceof TypeBeanOrUnderlying) {
                    EventType innerType = ((TypeBeanOrUnderlying) type).getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter = factory.getGetterBeanNested(indexedProp.getPropertyNameAtomic(), innerType, eventBeanTypedEventFactory);
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else if (type instanceof TypeBeanOrUnderlying[]) {
                    EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter = factory.getGetterIndexedUnderlyingArray(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), eventBeanTypedEventFactory, innerType, beanEventTypeFactory);
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else if (type instanceof EPTypeClass) {
                    EPTypeClass eptype = (EPTypeClass) type;
                    if (!eptype.getType().isArray()) {
                        return null;
                    }
                    EPTypeClass componentType = JavaClassHelper.getArrayComponentType(eptype);
                    EventPropertyGetterSPI indexedGetter = factory.getGetterIndexedClassArray(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), eventBeanTypedEventFactory, componentType, beanEventTypeFactory);
                    propertyGetterCache.put(propertyName, indexedGetter);
                    return indexedGetter;
                } else {
                    return null;
                }
            } else if (prop instanceof MappedProperty) {
                MappedProperty mappedProp = (MappedProperty) prop;
                Object type = nestableTypes.get(mappedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof EPTypeClass) {
                    if (JavaClassHelper.isImplementsInterface((EPTypeClass) type, Map.class)) {
                        return factory.getGetterMappedProperty(mappedProp.getPropertyNameAtomic(), mappedProp.getKey());
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = PropertyParser.unescapeBacktickForProperty(StringValue.unescapeDot(propertyName.substring(0, index)));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());
        boolean isRootedDynamic = false;

        // If the property is dynamic, remove the ? since the property type is defined without
        if (propertyMap.endsWith("?")) {
            propertyMap = propertyMap.substring(0, propertyMap.length() - 1);
            isRootedDynamic = true;
        }

        Object nestedType = nestableTypes.get(propertyMap);
        if (nestedType == null) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyMap);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof TypeBeanOrUnderlying[]) {
                    EventTypeSPI innerType = (EventTypeSPI) ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    EventPropertyGetterSPI innerGetter = innerType.getGetterSPI(propertyNested);
                    if (innerGetter == null) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter = factory.getGetterNestedEntryBeanArray(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), innerGetter, innerType, eventBeanTypedEventFactory);
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else if (type instanceof EventType[]) {
                    EventTypeSPI componentType = (EventTypeSPI) ((EventType[]) type)[0];
                    EventPropertyGetterSPI nestedGetter = componentType.getGetterSPI(propertyNested);
                    if (nestedGetter == null) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter = factory.getGetterIndexedEntryEventBeanArrayElement(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), nestedGetter);
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else if (type instanceof EPTypeClass) {
                    if (!((EPTypeClass) type).getType().isArray()) {
                        return null;
                    }
                    EPTypeClass componentType = JavaClassHelper.getArrayComponentType((EPTypeClass) type);
                    BeanEventType nestedEventType = beanEventTypeFactory.getCreateBeanType(componentType, publicFields);
                    final BeanEventPropertyGetter nestedGetter = (BeanEventPropertyGetter) nestedEventType.getGetterSPI(propertyNested);
                    if (nestedGetter == null) {
                        return null;
                    }
                    EPTypeClass propertyTypeGetter = (EPTypeClass) nestedEventType.getPropertyEPType(propertyNested);
                    // construct getter for nested property
                    EventPropertyGetterSPI indexGetter = factory.getGetterIndexedEntryPOJO(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, propertyTypeGetter);
                    propertyGetterCache.put(propertyName, indexGetter);
                    return indexGetter;
                } else {
                    return null;
                }
            } else if (property instanceof MappedProperty) {
                return null;    // Since no type information is available for the property
            } else {
                if (isRootedDynamic) {
                    Property prop = PropertyParser.parseAndWalk(propertyNested, true);
                    if (!isObjectArray) {
                        EventPropertyGetterSPI getterNested = factory.getGetterRootedDynamicNested(prop, eventBeanTypedEventFactory, beanEventTypeFactory);
                        EventPropertyGetterSPI dynamicGetter = factory.getGetterNestedPropertyProvidedGetterDynamic(nestableTypes, propertyMap, getterNested, eventBeanTypedEventFactory);
                        propertyGetterCache.put(propertyName, dynamicGetter);
                        return dynamicGetter;
                    }
                    return null;
                }
                return null;
            }
        }

        // The map contains another map, we resolve the property dynamically
        if (nestedType instanceof EPTypeClass && ((EPTypeClass) nestedType).getType() == Map.class) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyNested);
            MapEventPropertyGetter getterNestedMap = prop.getGetterMap(null, eventBeanTypedEventFactory, beanEventTypeFactory);
            if (getterNestedMap == null) {
                return null;
            }
            EventPropertyGetterSPI mapGetter = factory.getGetterNestedMapProp(propertyMap, getterNestedMap);
            propertyGetterCache.put(propertyName, mapGetter);
            return mapGetter;
        } else if (nestedType instanceof Map) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyNested);
            Map nestedTypes = (Map) nestedType;
            MapEventPropertyGetter getterNestedMap = prop.getGetterMap(nestedTypes, eventBeanTypedEventFactory, beanEventTypeFactory);
            if (getterNestedMap == null) {
                return null;
            }
            EventPropertyGetterSPI mapGetter = factory.getGetterNestedMapProp(propertyMap, getterNestedMap);
            propertyGetterCache.put(propertyName, mapGetter);
            return mapGetter;
        } else if (nestedType instanceof EPTypeClass) {
            // ask the nested class to resolve the property
            EPTypeClass simpleClass = (EPTypeClass) nestedType;
            if (simpleClass.getType().isArray()) {
                return null;
            }
            BeanEventType nestedEventType = beanEventTypeFactory.getCreateBeanType(simpleClass, publicFields);
            final BeanEventPropertyGetter nestedGetter = (BeanEventPropertyGetter) nestedEventType.getGetterSPI(propertyNested);
            if (nestedGetter == null) {
                return null;
            }

            EPTypeClass propertyType;
            EventPropertyDescriptor desc = nestedEventType.getPropertyDescriptor(propertyNested);
            if (desc == null) {
                propertyType = (EPTypeClass) nestedEventType.getPropertyEPType(propertyNested);
            } else {
                propertyType = (EPTypeClass) desc.getPropertyEPType();
            }

            // construct getter for nested property
            EventPropertyGetterSPI getter = factory.getGetterNestedPOJOProp(propertyMap, nestedGetter, eventBeanTypedEventFactory, beanEventTypeFactory, propertyType);
            propertyGetterCache.put(propertyName, getter);
            return getter;
        } else if (nestedType instanceof EventType) {
            // ask the nested class to resolve the property
            EventTypeSPI innerType = (EventTypeSPI) nestedType;
            final EventPropertyGetterSPI nestedGetter = innerType.getGetterSPI(propertyNested);
            if (nestedGetter == null) {
                return null;
            }

            // construct getter for nested property
            EventPropertyGetterSPI getter = factory.getGetterNestedEventBean(propertyMap, nestedGetter);
            propertyGetterCache.put(propertyName, getter);
            return getter;
        } else if (nestedType instanceof EventType[]) {
            EventType[] typeArray = (EventType[]) nestedType;
            EventPropertyGetterSPI beanArrGetter = factory.getGetterEventBeanArray(propertyMap, typeArray[0]);
            propertyGetterCache.put(propertyName, beanArrGetter);
            return beanArrGetter;
        } else if (nestedType instanceof TypeBeanOrUnderlying) {
            EventType innerType = ((TypeBeanOrUnderlying) nestedType).getEventType();
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            EventPropertyGetterSPI innerGetter = ((EventTypeSPI) innerType).getGetterSPI(propertyNested);
            if (innerGetter == null) {
                return null;
            }
            EventPropertyGetterSPI outerGetter = factory.getGetterNestedEntryBean(propertyMap, innerGetter, innerType, eventBeanTypedEventFactory);
            propertyGetterCache.put(propertyName, outerGetter);
            return outerGetter;
        } else if (nestedType instanceof TypeBeanOrUnderlying[]) {
            EventType innerType = ((TypeBeanOrUnderlying[]) nestedType)[0].getEventType();
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            EventPropertyGetterSPI innerGetter = ((EventTypeSPI) innerType).getGetterSPI(propertyNested);
            if (innerGetter == null) {
                return null;
            }
            EventPropertyGetterSPI outerGetter = factory.getGetterNestedEntryBeanArray(propertyMap, 0, innerGetter, innerType, eventBeanTypedEventFactory);
            propertyGetterCache.put(propertyName, outerGetter);
            return outerGetter;
        } else {
            String message = "Nestable type configuration encountered an unexpected value type of '"
                + nestedType.getClass() + " for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
            throw new PropertyAccessException(message);
        }
    }

    public static LinkedHashMap<String, Object> validateObjectArrayDef(String[] propertyNames, Object[] propertyTypes) {
        if (propertyNames.length != propertyTypes.length) {
            throw new ConfigurationException("Number of property names and property types do not match, found " + propertyNames.length + " property names and " +
                propertyTypes.length + " property types");
        }

        // validate property names for no-duplicates
        Set<String> propertyNamesSet = new HashSet<String>();
        LinkedHashMap<String, Object> propertyTypesMap = new LinkedHashMap<String, Object>();
        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            if (propertyNamesSet.contains(propertyName)) {     // duplicate prop check
                throw new ConfigurationException("Property '" + propertyName + "' is listed twice in the type definition");
            }
            propertyNamesSet.add(propertyName);
            propertyTypesMap.put(propertyName, propertyTypes[i]);
        }
        return propertyTypesMap;
    }

    public static WriteablePropertyDescriptor findWritable(String propertyName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(propertyName)) {
                return writable;
            }
        }
        return null;
    }

    public static TimestampPropertyDesc validatedDetermineTimestampProps(EventType type, String startProposed, String endProposed, EventType[] superTypes)
        throws EPException {
        // determine start&end timestamp as inherited
        String startTimestampPropertyName = startProposed;
        String endTimestampPropertyName = endProposed;

        if (superTypes != null && superTypes.length > 0) {
            for (EventType superType : superTypes) {
                if (superType.getStartTimestampPropertyName() != null) {
                    if (startTimestampPropertyName != null && !startTimestampPropertyName.equals(superType.getStartTimestampPropertyName())) {
                        throw getExceptionTimestampInherited("start", startTimestampPropertyName, superType.getStartTimestampPropertyName(), superType);
                    }
                    startTimestampPropertyName = superType.getStartTimestampPropertyName();
                }
                if (superType.getEndTimestampPropertyName() != null) {
                    if (endTimestampPropertyName != null && !endTimestampPropertyName.equals(superType.getEndTimestampPropertyName())) {
                        throw getExceptionTimestampInherited("end", endTimestampPropertyName, superType.getEndTimestampPropertyName(), superType);
                    }
                    endTimestampPropertyName = superType.getEndTimestampPropertyName();
                }
            }
        }

        validateTimestampProperties(type, startTimestampPropertyName, endTimestampPropertyName);
        return new TimestampPropertyDesc(startTimestampPropertyName, endTimestampPropertyName);
    }

    private static EPException getExceptionTimestampInherited(String tstype, String firstName, String secondName, EventType superType) {
        String message = "Event type declares " + tstype + " timestamp as property '" + firstName + "' however inherited event type '" + superType.getName() +
            "' declares " + tstype + " timestamp as property '" + secondName + "'";
        return new EPException(message);
    }

    private static void addRecursiveSupertypes(Set<EventType> superTypes, EventType child) {
        if (child.getSuperTypes() != null) {
            for (int i = 0; i < child.getSuperTypes().length; i++) {
                superTypes.add(child.getSuperTypes()[i]);
                addRecursiveSupertypes(superTypes, child.getSuperTypes()[i]);
            }
        }
    }

    public static String disallowedAtTypeMessage() {
        return "The @type annotation is only allowed when the invocation target returns EventBean instances";
    }

    public static void validateEventBeanClassVisibility(Class clazz) {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new EventAdapterException("Event class '" + clazz.getName() + "' does not have public visibility");
        }
    }

    public static EventPropertyGetterSPI[] getGetters(EventType eventType, String[] props) {
        EventPropertyGetterSPI[] getters = new EventPropertyGetterSPI[props.length];
        EventTypeSPI spi = (EventTypeSPI) eventType;
        for (int i = 0; i < getters.length; i++) {
            getters[i] = spi.getGetterSPI(props[i]);
        }
        return getters;
    }

    public static EPType[] getPropertyTypesEPType(EventType eventType, String[] props) {
        EPType[] types = new EPType[props.length];
        for (int i = 0; i < props.length; i++) {
            types[i] = eventType.getPropertyEPType(props[i]);
        }
        return types;
    }

    public static EventType[] shiftRight(EventType[] src) {
        EventType[] types = new EventType[src.length + 1];
        System.arraycopy(src, 0, types, 1, src.length);
        return types;
    }

    public static String getAdapterForMethodName(EventType eventType) {
        if (eventType instanceof MapEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDMAP;
        }
        if (eventType instanceof ObjectArrayEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDOBJECTARRAY;
        }
        if (eventType instanceof BeanEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDBEAN;
        }
        if (eventType instanceof BaseXMLEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDDOM;
        }
        if (eventType instanceof AvroSchemaEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDAVRO;
        }
        if (eventType instanceof JsonEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDJSON;
        }
        if (eventType instanceof WrapperEventType) {
            return EventBeanTypedEventFactory.ADAPTERFORTYPEDWRAPPER;
        }
        throw new IllegalArgumentException("Unrecognized event type " + eventType);
    }

    public static Class getPropertyTypeAsClass(EPType type) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return null;
        }
        return ((EPTypeClass) type).getType();
    }

    public static class TimestampPropertyDesc {
        private final String start;
        private final String end;

        public TimestampPropertyDesc(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }

    public static String getMessageExpecting(String eventTypeName, EventType existingType, String typeOfEventType) {
        String message = "Event type named '" + eventTypeName + "' has not been defined or is not a " + typeOfEventType + " event type";
        if (existingType != null) {
            message += ", the name '" + eventTypeName + "' refers to a " + ClassHelperPrint.getClassNameFullyQualPretty(existingType.getUnderlyingType()) + " event type";
        } else {
            message += ", the name '" + eventTypeName + "' has not been defined as an event type";
        }
        return message;
    }


    public static void validateTypeObjectArray(String eventTypeName, EventType type) {
        if (!(type instanceof ObjectArrayEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "Object-array"));
        }
    }

    public static void validateTypeBean(String eventTypeName, EventType type) {
        if (!(type instanceof BeanEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "Bean-type"));
        }
    }

    public static void validateTypeMap(String eventTypeName, EventType type) {
        if (!(type instanceof MapEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "Map-type"));
        }
    }

    public static void validateTypeJson(String eventTypeName, EventType type) {
        if (!(type instanceof JsonEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "Json-type"));
        }
    }

    public static void validateTypeXMLDOM(String eventTypeName, EventType type) {
        if (!(type instanceof BaseXMLEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "XML-DOM-type"));
        }
    }

    public static void validateTypeAvro(String eventTypeName, EventType type) {
        if (!(type instanceof AvroSchemaEventType)) {
            throw new EPException(getMessageExpecting(eventTypeName, type, "Avro"));
        }
    }

    public static void validateModifiers(String eventTypeName, EventTypeBusModifier eventBusVisibility, NameAccessModifier nameAccessModifier) throws ExprValidationException {
        if (eventBusVisibility != EventTypeBusModifier.BUS) {
            return;
        }
        if (nameAccessModifier != NameAccessModifier.PRECONFIGURED && nameAccessModifier != NameAccessModifier.PUBLIC) {
            throw new ExprValidationException("Event type '" + eventTypeName + "' with bus-visibility requires the public access modifier for the event type");
        }
    }

    public static EventBean[] typeCast(List<EventBean> events, EventType targetType, EventBeanTypedEventFactory eventAdapterService, EventTypeAvroHandler eventTypeAvroHandler) {
        EventBean[] convertedArray = new EventBean[events.size()];
        int count = 0;
        for (EventBean theEvent : events) {
            EventBean converted;
            if (theEvent instanceof DecoratingEventBean) {
                DecoratingEventBean wrapper = (DecoratingEventBean) theEvent;
                if (targetType instanceof MapEventType) {
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.putAll(wrapper.getDecoratingProperties());
                    for (EventPropertyDescriptor propDesc : wrapper.getUnderlyingEvent().getEventType().getPropertyDescriptors()) {
                        props.put(propDesc.getPropertyName(), wrapper.getUnderlyingEvent().get(propDesc.getPropertyName()));
                    }
                    converted = eventAdapterService.adapterForTypedMap(props, targetType);
                } else {
                    converted = eventAdapterService.adapterForTypedWrapper(wrapper.getUnderlyingEvent(), wrapper.getDecoratingProperties(), targetType);
                }
            } else if ((theEvent.getEventType() instanceof MapEventType) && (targetType instanceof MapEventType)) {
                MappedEventBean mapEvent = (MappedEventBean) theEvent;
                converted = eventAdapterService.adapterForTypedMap(mapEvent.getProperties(), targetType);
            } else if ((theEvent.getEventType() instanceof MapEventType) && (targetType instanceof WrapperEventType)) {
                converted = eventAdapterService.adapterForTypedWrapper(theEvent, Collections.EMPTY_MAP, targetType);
            } else if ((theEvent.getEventType() instanceof BeanEventType) && (targetType instanceof BeanEventType)) {
                converted = eventAdapterService.adapterForTypedBean(theEvent.getUnderlying(), targetType);
            } else if (theEvent.getEventType() instanceof ObjectArrayEventType && targetType instanceof ObjectArrayEventType) {
                Object[] convertedObjectArray = ObjectArrayEventType.convertEvent(theEvent, (ObjectArrayEventType) targetType);
                converted = eventAdapterService.adapterForTypedObjectArray(convertedObjectArray, targetType);
            } else if (theEvent.getEventType() instanceof AvroSchemaEventType && targetType instanceof AvroSchemaEventType) {
                Object convertedGenericRecord = eventTypeAvroHandler.convertEvent(theEvent, (AvroSchemaEventType) targetType);
                converted = eventAdapterService.adapterForTypedAvro(convertedGenericRecord, targetType);
            } else if ((theEvent.getEventType() instanceof JsonEventType) && (targetType instanceof JsonEventType)) {
                Object und = convertJsonEvents(theEvent, (JsonEventType) targetType);
                converted = eventAdapterService.adapterForTypedJson(und, targetType);
            } else {
                throw new EPException("Unknown event type " + theEvent.getEventType());
            }
            convertedArray[count] = converted;
            count++;
        }
        return convertedArray;
    }

    private static Object convertJsonEvents(EventBean theEvent, JsonEventType targetType) {
        Object target = targetType.getDelegateFactory().newUnderlying();
        Object source = theEvent.getUnderlying();
        JsonEventType sourceType = (JsonEventType) theEvent.getEventType();
        for (Map.Entry<String, JsonUnderlyingField> entry : targetType.getDetail().getFieldDescriptors().entrySet()) {
            JsonUnderlyingField sourceField = entry.getValue();
            JsonUnderlyingField targetField = sourceType.getDetail().getFieldDescriptors().get(entry.getKey());
            if (targetField == null) {
                continue;
            }
            Object value = sourceType.getDelegateFactory().getValue(sourceField.getPropertyNumber(), source);
            targetType.getDelegateFactory().setValue(targetField.getPropertyNumber(), value, target);
        }
        return target;
    }

    public static EventTypeForgablesPair createNonVariantType(boolean isAnonymous, CreateSchemaDesc spec, StatementBaseInfo base, StatementCompileTimeServices services)
        throws ExprValidationException {
        if (spec.getAssignedType() == CreateSchemaDesc.AssignedType.VARIANT) {
            throw new IllegalStateException("Variant type is not allowed in this context");
        }
        Annotation[] annotations = base.getStatementRawInfo().getAnnotations();

        NameAccessModifier visibility;
        EventTypeBusModifier eventBusVisibility;
        if (isAnonymous) {
            visibility = NameAccessModifier.TRANSIENT;
            eventBusVisibility = EventTypeBusModifier.NONBUS;
        } else {
            visibility = services.getModuleVisibilityRules().getAccessModifierEventType(base.getStatementRawInfo(), spec.getSchemaName());
            eventBusVisibility = services.getModuleVisibilityRules().getBusModifierEventType(base.getStatementRawInfo(), spec.getSchemaName());
            EventTypeUtility.validateModifiers(spec.getSchemaName(), eventBusVisibility, visibility);
        }

        EventType eventType;
        List<StmtClassForgeableFactory> additionalForgeables = Collections.emptyList();
        if (spec.getTypes().isEmpty() && spec.getAssignedType() != CreateSchemaDesc.AssignedType.XML) {
            EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(annotations, services.getConfiguration(), spec.getAssignedType());
            Map<String, Object> typing = EventTypeUtility.buildType(spec.getColumns(), spec.getCopyFrom(), services.getClasspathImportServiceCompileTime(), services.getClassProvidedClasspathExtension(), services.getEventTypeCompileTimeResolver());
            Map<String, Object> compiledTyping = EventTypeUtility.compileMapTypeProperties(typing, services.getEventTypeCompileTimeResolver());

            ConfigurationCommonEventTypeWithSupertype config;
            if (representation == EventUnderlyingType.MAP) {
                config = new ConfigurationCommonEventTypeMap();
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                config = new ConfigurationCommonEventTypeObjectArray();
            } else if (representation == EventUnderlyingType.AVRO) {
                config = new ConfigurationCommonEventTypeAvro();
            } else if (representation == EventUnderlyingType.JSON) {
                config = new ConfigurationCommonEventTypeJson();
            } else {
                throw new IllegalStateException("Unrecognized representation '" + representation + "'");
            }

            if (spec.getInherits() != null) {
                config.getSuperTypes().addAll(spec.getInherits());
                if (spec.getInherits().size() > 1 && (representation == EventUnderlyingType.OBJECTARRAY || representation == EventUnderlyingType.JSON)) {
                    throw new ExprValidationException(ConfigurationCommonEventTypeObjectArray.SINGLE_SUPERTYPE_MSG);
                }
            }
            config.setStartTimestampPropertyName(spec.getStartTimestampProperty());
            config.setEndTimestampPropertyName(spec.getEndTimestampProperty());

            Function<EventTypeApplicationType, EventTypeMetadata> metadataFunc = appType -> new EventTypeMetadata(spec.getSchemaName(), base.getModuleName(), EventTypeTypeClass.STREAM, appType, visibility, eventBusVisibility, false, EventTypeIdPair.unassigned());
            if (representation == EventUnderlyingType.MAP) {
                Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(config.getSuperTypes(), EventUnderlyingType.MAP, services.getEventTypeCompileTimeResolver());
                EventTypeMetadata metadata = metadataFunc.apply(EventTypeApplicationType.MAP);
                eventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, compiledTyping, st.getFirst(), st.getSecond(), config.getStartTimestampPropertyName(), config.getEndTimestampPropertyName(), services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(config.getSuperTypes(), EventUnderlyingType.OBJECTARRAY, services.getEventTypeCompileTimeResolver());
                EventTypeMetadata metadata = metadataFunc.apply(EventTypeApplicationType.OBJECTARR);
                eventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata, compiledTyping, st.getFirst(), st.getSecond(), config.getStartTimestampPropertyName(), config.getEndTimestampPropertyName(), services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
            } else if (representation == EventUnderlyingType.AVRO) {
                Pair<EventType[], Set<EventType>> avroSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(config.getSuperTypes(), EventUnderlyingType.AVRO, services.getEventTypeCompileTimeResolver());
                EventTypeMetadata metadata = metadataFunc.apply(EventTypeApplicationType.AVRO);
                eventType = services.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata, services.getEventTypeCompileTimeResolver(), services.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), compiledTyping, annotations, (ConfigurationCommonEventTypeAvro) config, avroSuperTypes.getFirst(), avroSuperTypes.getSecond(), base.getStatementName());
            } else if (representation == EventUnderlyingType.JSON) {
                Pair<EventType[], Set<EventType>> st = EventTypeUtility.getSuperTypesDepthFirst(config.getSuperTypes(), EventUnderlyingType.JSON, services.getEventTypeCompileTimeResolver());
                EventTypeMetadata metadata = metadataFunc.apply(EventTypeApplicationType.JSON);
                EventTypeForgablesPair desc = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata, compiledTyping, st, config, base.getStatementRawInfo(), services);
                eventType = desc.getEventType();
                additionalForgeables = desc.getAdditionalForgeables();
            } else {
                throw new IllegalStateException("Unrecognized representation " + representation);
            }
        } else if (spec.getAssignedType() == CreateSchemaDesc.AssignedType.XML) {
            if (!spec.getColumns().isEmpty()) {
                throw new ExprValidationException("Create-XML-Schema does not allow specifying columns, use @" + XMLSchemaField.class.getSimpleName() + " instead");
            }
            if (!spec.getCopyFrom().isEmpty()) {
                throw new ExprValidationException("Create-XML-Schema does not allow copy-from");
            }
            if (!spec.getInherits().isEmpty()) {
                throw new ExprValidationException("Create-XML-Schema does not allow inherits");
            }
            ConfigurationCommonEventTypeXMLDOM config = CreateSchemaXMLHelper.configure(base, services);
            SchemaModel schemaModel = null;
            if ((config.getSchemaResource() != null) || (config.getSchemaText() != null)) {
                try {
                    schemaModel = services.getEventTypeXMLXSDHandler().loadAndMap(config.getSchemaResource(), config.getSchemaText(), services.getClasspathImportServiceCompileTime());
                } catch (Exception ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
            }
            boolean propertyAgnostic = schemaModel == null;
            EventTypeMetadata metadata = new EventTypeMetadata(spec.getSchemaName(), base.getModuleName(), EventTypeTypeClass.STREAM, EventTypeApplicationType.XML, visibility, eventBusVisibility, propertyAgnostic, EventTypeIdPair.unassigned());
            config.setStartTimestampPropertyName(spec.getStartTimestampProperty());
            config.setEndTimestampPropertyName(spec.getEndTimestampProperty());
            eventType = EventTypeFactoryImpl.INSTANCE.createXMLType(metadata, config, schemaModel, null, metadata.getName(), services.getBeanEventTypeFactoryPrivate(), services.getXmlFragmentEventTypeFactory(), null, services.getEventTypeXMLXSDHandler());
        } else {
            // Java Object/Bean/POJO type definition
            if (spec.getCopyFrom() != null && !spec.getCopyFrom().isEmpty()) {
                throw new ExprValidationException("Copy-from types are not allowed with class-provided types");
            }
            if (spec.getTypes().size() != 1) {
                throw new IllegalStateException("Multiple types provided");
            }
            try {
                // use the existing configuration, if any, possibly adding the start and end timestamps
                String name = spec.getTypes().iterator().next();
                ClassDescriptor descriptor = ClassDescriptor.parseTypeText(name);
                Class clazz = services.getClasspathImportServiceCompileTime().resolveClassForBeanEventType(descriptor.getClassIdentifier());
                EPTypeClass parameterized = ClasspathImportEPTypeUtil.parameterizeType(false, clazz, descriptor, services.getClasspathImportServiceCompileTime(), services.getClassProvidedClasspathExtension());
                BeanEventTypeStem stem = services.getBeanEventTypeStemService().getCreateStem(parameterized, null);
                EventTypeMetadata metadata = new EventTypeMetadata(spec.getSchemaName(), base.getModuleName(), EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, visibility, eventBusVisibility, false, EventTypeIdPair.unassigned());
                EventType[] superTypes = getSuperTypes(stem.getSuperTypes(), services);
                Set<EventType> deepSuperTypes = getDeepSupertypes(stem.getDeepSuperTypes(), services);
                eventType = new BeanEventType(stem, metadata, services.getBeanEventTypeFactoryPrivate(), superTypes, deepSuperTypes, spec.getStartTimestampProperty(), spec.getEndTimestampProperty());
            } catch (ClasspathImportException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }
        }

        services.getEventTypeCompileTimeRegistry().newType(eventType);
        return new EventTypeForgablesPair(eventType, additionalForgeables);
    }

    private static boolean allowPopulate(EventTypeSPI typeSPI) {
        EventTypeMetadata metadata = typeSPI.getMetadata();
        if (metadata.getTypeClass() == EventTypeTypeClass.STREAM || metadata.getTypeClass() == EventTypeTypeClass.APPLICATION) {
            return true;
        }
        if (metadata.getTypeClass() != EventTypeTypeClass.STATEMENTOUT &&
            metadata.getTypeClass() != EventTypeTypeClass.TABLE_INTERNAL) {
            return false;
        }
        return true;
    }

    private static EventType[] getSuperTypes(EPTypeClass[] superTypes, StatementCompileTimeServices services) {
        if (superTypes == null || superTypes.length == 0) {
            return null;
        }
        EventType[] types = new EventType[superTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = resolveOrCreateType(superTypes[i], services);
        }
        return types;
    }

    private static Set<EventType> getDeepSupertypes(Set<EPTypeClass> superTypes, StatementCompileTimeServices services) {
        if (superTypes == null || superTypes.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<EventType> supers = new LinkedHashSet<>(4);
        for (EPTypeClass clazz : superTypes) {
            supers.add(resolveOrCreateType(clazz, services));
        }
        return supers;
    }

    private static EventType resolveOrCreateType(EPTypeClass clazz, StatementCompileTimeServices services) {
        services.getEventTypeCompileTimeResolver();

        // find module-own type
        Collection<EventType> moduleTypes = services.getEventTypeCompileTimeRegistry().getNewTypesAdded();
        for (EventType eventType : moduleTypes) {
            if (matches(eventType, clazz)) {
                return eventType;
            }
        }

        // find path types
        PathRegistry<String, EventType> pathRegistry = services.getEventTypeCompileTimeResolver().getPath();
        List<EventType> found = new ArrayList<>();
        pathRegistry.traverse(eventType -> {
            if (matches(eventType, clazz)) {
                found.add(eventType);
            }
        });
        if (found.size() > 1) {
            throw new EPException("Found multiple parent types in path for classs '" + clazz + "'");
        }
        if (!found.isEmpty()) {
            return found.get(0);
        }

        return services.getBeanEventTypeFactoryPrivate().getCreateBeanType(clazz, false);
    }

    private static boolean matches(EventType eventType, EPTypeClass clazz) {
        if (!(eventType instanceof BeanEventType)) {
            return false;
        }
        BeanEventType beanEventType = (BeanEventType) eventType;
        return beanEventType.getStem().getClazz().getType() == clazz.getType();
    }

    public static EventBeanAdapterFactory getAdapterFactoryForType(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTypeAvroHandler eventTypeAvroHandler) {
        if (eventType instanceof BeanEventType) {
            return new EventBeanAdapterFactoryBean(eventType, eventBeanTypedEventFactory);
        }
        if (eventType instanceof ObjectArrayEventType) {
            return new EventBeanAdapterFactoryObjectArray(eventType, eventBeanTypedEventFactory);
        }
        if (eventType instanceof MapEventType) {
            return new EventBeanAdapterFactoryMap(eventType, eventBeanTypedEventFactory);
        }
        if (eventType instanceof BaseXMLEventType) {
            return new EventBeanAdapterFactoryXml(eventType, eventBeanTypedEventFactory);
        }
        if (eventType instanceof AvroSchemaEventType) {
            return new EventBeanAdapterFactoryAvro(eventType, eventTypeAvroHandler);
        }
        if (eventType instanceof JsonEventType) {
            return new EventBeanAdapterFactoryJson(eventType, eventBeanTypedEventFactory);
        }
        if (eventType instanceof WrapperEventType) {
            WrapperEventType wrapperEventType = (WrapperEventType) eventType;
            EventBeanAdapterFactory factoryWrapped = getAdapterFactoryForType(wrapperEventType.getUnderlyingEventType(), eventBeanTypedEventFactory, eventTypeAvroHandler);
            if (wrapperEventType.isNoMapProperties()) {
                return new EventBeanAdapterFactoryWrapperNoProps(wrapperEventType, eventBeanTypedEventFactory, factoryWrapped);
            }
            return new EventBeanAdapterFactoryWrapperWithProps(wrapperEventType, eventBeanTypedEventFactory, factoryWrapped);
        }
        throw new EventAdapterException("Event type '" + eventType.getName() + "' is not a runtime-native event type");
    }

    public static class EventBeanAdapterFactoryBean implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

        public EventBeanAdapterFactoryBean(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
            this.eventType = eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventBeanTypedEventFactory.adapterForTypedBean(underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryMap implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

        public EventBeanAdapterFactoryMap(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
            this.eventType = eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventBeanTypedEventFactory.adapterForTypedMap((Map<String, Object>) underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryObjectArray implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

        public EventBeanAdapterFactoryObjectArray(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
            this.eventType = eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventBeanTypedEventFactory.adapterForTypedObjectArray((Object[]) underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryXml implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

        public EventBeanAdapterFactoryXml(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
            this.eventType = eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventBeanTypedEventFactory.adapterForTypedDOM((Node) underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryAvro implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventTypeAvroHandler eventTypeAvroHandler;

        public EventBeanAdapterFactoryAvro(EventType eventType, EventTypeAvroHandler eventTypeAvroHandler) {
            this.eventType = eventType;
            this.eventTypeAvroHandler = eventTypeAvroHandler;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventTypeAvroHandler.adapterForTypeAvro(underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryJson implements EventBeanAdapterFactory {
        private final EventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

        public EventBeanAdapterFactoryJson(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
            this.eventType = eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        }

        public EventBean makeAdapter(Object underlying) {
            return eventBeanTypedEventFactory.adapterForTypedJson(underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryWrapperWithProps implements EventBeanAdapterFactory {
        private final WrapperEventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
        private final EventBeanAdapterFactory factoryWrapped;

        public EventBeanAdapterFactoryWrapperWithProps(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventBeanAdapterFactory factoryWrapped) {
            this.eventType = (WrapperEventType) eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
            this.factoryWrapped = factoryWrapped;
        }

        public EventBean makeAdapter(Object underlying) {
            Pair<Object, Map<String, Object>> pair = (Pair<Object, Map<String, Object>>) underlying;
            EventBean inner = factoryWrapped.makeAdapter(pair.getFirst());
            return eventBeanTypedEventFactory.adapterForTypedWrapper(inner, pair.getSecond(), eventType);
        }
    }

    public static class EventBeanAdapterFactoryWrapperNoProps implements EventBeanAdapterFactory {
        private final WrapperEventType eventType;
        private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
        private final EventBeanAdapterFactory factoryWrapped;

        public EventBeanAdapterFactoryWrapperNoProps(EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventBeanAdapterFactory factoryWrapped) {
            this.eventType = (WrapperEventType) eventType;
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
            this.factoryWrapped = factoryWrapped;
        }

        public EventBean makeAdapter(Object underlying) {
            EventBean inner = factoryWrapped.makeAdapter(underlying);
            return eventBeanTypedEventFactory.adapterForTypedWrapper(inner, Collections.emptyMap(), eventType);
        }
    }
}
