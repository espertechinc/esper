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
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.wrap.WrapperGetterIndexed;
import com.espertech.esper.common.internal.event.wrap.WrapperGetterMapped;
import com.espertech.esper.common.internal.event.wrap.WrapperMapPropertyGetter;
import com.espertech.esper.common.internal.event.wrap.WrapperUnderlyingPropertyGetter;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.*;

import static com.espertech.esper.common.client.util.NameAccessModifier.PRIVATE;
import static com.espertech.esper.common.client.util.NameAccessModifier.TRANSIENT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * An event type that adds zero or more fields to an existing event type.
 * <p>
 * The additional fields are represented as a Map. Any queries to event properties are first
 * held against the additional fields, and secondly are handed through to the underlying event.
 * <p>
 * If this event type is to add information to another wrapper event type (wrapper to wrapper), then it is the
 * responsibility of the creating logic to use the existing event type and add to it.
 * <p>
 * Uses a the map event type {@link com.espertech.esper.common.internal.event.map.MapEventType} to represent the mapped properties. This is because the additional properties
 * can also be beans or complex types and the Map event type handles these nicely.
 */
public class WrapperEventType implements EventTypeSPI {
    /**
     * event type metadata
     */
    protected EventTypeMetadata metadata;

    /**
     * The underlying wrapped event type.
     */
    protected final EventType underlyingEventType;

    /**
     * The map event type that provides the additional properties.
     */
    protected final MapEventType underlyingMapType;

    private String[] propertyNames;
    private EventPropertyDescriptor[] propertyDesc;
    private Map<String, EventPropertyDescriptor> propertyDescriptorMap;

    private final boolean isNoMapProperties;
    private final Map<String, EventPropertyGetterSPI> propertyGetterCache;
    protected final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private EventPropertyDescriptor[] writableProperties;
    private Map<String, Pair<EventPropertyDescriptor, EventPropertyWriterSPI>> writers;

    private String startTimestampPropertyName;
    private String endTimestampPropertyName;
    private int numPropertiesUnderlyingType;

    protected WrapperEventType(EventTypeMetadata metadata, EventType underlyingEventType, Map<String, Object> properties, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        checkForRepeatedPropertyNames(underlyingEventType, properties);

        this.metadata = metadata;
        this.underlyingEventType = underlyingEventType;
        String innerName = EventTypeNameUtil.getWrapperInnerTypeName(metadata.getName());
        EventTypeIdPair ids = computeIdFromWrapped(metadata.getAccessModifier(), innerName, metadata);
        EventTypeMetadata metadataMapType = new EventTypeMetadata(innerName, this.metadata.getModuleName(), metadata.getTypeClass(), metadata.getApplicationType(), metadata.getAccessModifier(), EventTypeBusModifier.NONBUS, false, ids);
        this.underlyingMapType = new MapEventType(metadataMapType, properties, null, null, null, null, beanEventTypeFactory);
        this.isNoMapProperties = properties.isEmpty();
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        propertyGetterCache = new HashMap<String, EventPropertyGetterSPI>();

        updatePropertySet();

        if (metadata.getTypeClass() == EventTypeTypeClass.NAMED_WINDOW) {
            startTimestampPropertyName = underlyingEventType.getStartTimestampPropertyName();
            endTimestampPropertyName = underlyingEventType.getEndTimestampPropertyName();
            EventTypeUtility.validateTimestampProperties(this, startTimestampPropertyName, endTimestampPropertyName);
        }
    }

    public void setMetadataId(long publicId, long protectedId) {
        metadata = metadata.withIds(publicId, protectedId);
    }

    private void checkInitProperties() {
        if (numPropertiesUnderlyingType != underlyingEventType.getPropertyDescriptors().length) {
            updatePropertySet();
        }
    }

    private void updatePropertySet() {
        PropertyDescriptorComposite compositeProperties = getCompositeProperties(underlyingEventType, underlyingMapType);
        propertyNames = compositeProperties.getPropertyNames();
        propertyDescriptorMap = compositeProperties.getPropertyDescriptorMap();
        propertyDesc = compositeProperties.getDescriptors();
        numPropertiesUnderlyingType = underlyingEventType.getPropertyDescriptors().length;
    }

    private static PropertyDescriptorComposite getCompositeProperties(EventType underlyingEventType, MapEventType underlyingMapType) {
        List<String> propertyNames = new ArrayList<String>();
        propertyNames.addAll(Arrays.asList(underlyingEventType.getPropertyNames()));
        propertyNames.addAll(Arrays.asList(underlyingMapType.getPropertyNames()));
        String[] propertyNamesArr = propertyNames.toArray(new String[propertyNames.size()]);

        List<EventPropertyDescriptor> propertyDesc = new ArrayList<EventPropertyDescriptor>();
        HashMap<String, EventPropertyDescriptor> propertyDescriptorMap = new HashMap<String, EventPropertyDescriptor>();
        for (EventPropertyDescriptor eventProperty : underlyingEventType.getPropertyDescriptors()) {
            propertyDesc.add(eventProperty);
            propertyDescriptorMap.put(eventProperty.getPropertyName(), eventProperty);
        }
        for (EventPropertyDescriptor mapProperty : underlyingMapType.getPropertyDescriptors()) {
            propertyDesc.add(mapProperty);
            propertyDescriptorMap.put(mapProperty.getPropertyName(), mapProperty);
        }
        EventPropertyDescriptor[] propertyDescArr = propertyDesc.toArray(new EventPropertyDescriptor[propertyDesc.size()]);
        return new PropertyDescriptorComposite(propertyDescriptorMap, propertyNamesArr, propertyDescArr);
    }

    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return null;
    }

    public Set<EventType> getDeepSuperTypesAsSet() {
        return Collections.emptySet();
    }

    public String getName() {
        return metadata.getName();
    }

    public EventPropertyGetterSPI getGetterSPI(String property) {
        EventPropertyGetterSPI cachedGetter = propertyGetterCache.get(property);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        if (underlyingMapType.isProperty(property) && (property.indexOf('?') == -1)) {
            EventPropertyGetterSPI mapGetter = underlyingMapType.getGetterSPI(property);
            WrapperMapPropertyGetter getter = new WrapperMapPropertyGetter(this, eventBeanTypedEventFactory, underlyingMapType, mapGetter);
            propertyGetterCache.put(property, getter);
            return getter;
        } else if (underlyingEventType.isProperty(property)) {
            EventPropertyGetterSPI underlyingGetter = ((EventTypeSPI) underlyingEventType).getGetterSPI(property);
            WrapperUnderlyingPropertyGetter getter = new WrapperUnderlyingPropertyGetter(this, underlyingGetter);
            propertyGetterCache.put(property, getter);
            return getter;
        } else {
            return null;
        }
    }

    public EventPropertyGetter getGetter(final String propertyName) {
        return getGetterSPI(propertyName);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String mappedProperty) {
        final EventPropertyGetterMappedSPI undMapped = ((EventTypeSPI) underlyingEventType).getGetterMappedSPI(mappedProperty);
        if (undMapped != null) {
            return new WrapperGetterMapped(undMapped);
        }
        final EventPropertyGetterMappedSPI decoMapped = underlyingMapType.getGetterMappedSPI(mappedProperty);
        if (decoMapped != null) {
            return new EventPropertyGetterMappedSPI() {
                public Object get(EventBean theEvent, String mapKey) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    EventBean wrapped = eventBeanTypedEventFactory.adapterForTypedMap(map, underlyingMapType);
                    return decoMapped.get(wrapped, mapKey);
                }

                public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
                    CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
                    CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(underlyingEventType, EPStatementInitServices.REF));
                    CodegenMethod method = codegenMethodScope.makeChild(Object.class, WrapperEventType.class, codegenClassScope).addParam(EventBean.class, "theEvent").addParam(String.class, "mapKey").getBlock()
                            .declareVar(DecoratingEventBean.class, "wrapperEvent", cast(DecoratingEventBean.class, ref("theEvent")))
                            .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                            .declareVar(EventBean.class, "wrapped", exprDotMethod(factory, "adapterForTypedMap", ref("map"), eventType))
                            .methodReturn(decoMapped.eventBeanGetMappedCodegen(codegenMethodScope, codegenClassScope, ref("wrapped"), ref("mapKey")));
                    return localMethodBuild(method).pass(beanExpression).pass(key).call();
                }
            };
        }
        return null;
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedProperty) {
        return getGetterMappedSPI(mappedProperty);
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        return getGetterIndexedSPI(indexedPropertyName);
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String indexedProperty) {
        final EventPropertyGetterIndexedSPI undIndexed = ((EventTypeSPI) underlyingEventType).getGetterIndexedSPI(indexedProperty);
        if (undIndexed != null) {
            return new WrapperGetterIndexed(undIndexed);
        }
        final EventPropertyGetterIndexedSPI decoIndexed = underlyingMapType.getGetterIndexedSPI(indexedProperty);
        if (decoIndexed != null) {
            return new EventPropertyGetterIndexedSPI() {
                public Object get(EventBean theEvent, int index) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    EventBean wrapped = eventBeanTypedEventFactory.adapterForTypedMap(map, underlyingMapType);
                    return decoIndexed.get(wrapped, index);
                }

                public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
                    CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
                    CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(underlyingEventType, EPStatementInitServices.REF));
                    CodegenMethod method = codegenMethodScope.makeChild(Object.class, WrapperEventType.class, codegenClassScope).addParam(EventBean.class, "theEvent").addParam(int.class, "index").getBlock()
                            .declareVar(DecoratingEventBean.class, "wrapperEvent", cast(DecoratingEventBean.class, ref("theEvent")))
                            .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                            .declareVar(EventBean.class, "wrapped", exprDotMethod(factory, "adapterForTypedMap", ref("map"), eventType))
                            .methodReturn(decoIndexed.eventBeanGetIndexedCodegen(codegenMethodScope, codegenClassScope, ref("wrapped"), ref("index")));
                    return localMethodBuild(method).pass(beanExpression).pass(key).call();
                }
            };
        }
        return null;
    }

    public String[] getPropertyNames() {
        checkInitProperties();
        return propertyNames;
    }

    public Class getPropertyType(String property) {
        if (underlyingEventType.isProperty(property)) {
            return underlyingEventType.getPropertyType(property);
        } else if (underlyingMapType.isProperty(property)) {
            return underlyingMapType.getPropertyType(property);
        } else {
            return null;
        }
    }

    public EventType[] getSuperTypes() {
        return null;
    }

    public Class getUnderlyingType() {
        // If the additional properties are empty, such as when wrapping a native event by means of wildcard-only select
        // then the underlying type is simply the wrapped type.
        if (isNoMapProperties) {
            return underlyingEventType.getUnderlyingType();
        } else {
            return Pair.class;
        }
    }

    /**
     * Returns the wrapped event type.
     *
     * @return wrapped type
     */
    public EventType getUnderlyingEventType() {
        return underlyingEventType;
    }

    /**
     * Returns the map type.
     *
     * @return map type providing additional properties.
     */
    public MapEventType getUnderlyingMapType() {
        return underlyingMapType;
    }

    public boolean isProperty(String property) {
        return underlyingEventType.isProperty(property) ||
                underlyingMapType.isProperty(property);
    }

    public String toString() {
        return "WrapperEventType " +
                "name=" + getName() + " " +
                "underlyingEventType=(" + underlyingEventType + ") " +
                "underlyingMapType=(" + underlyingMapType + ")";
    }

    public ExprValidationException equalsCompareType(EventType otherEventType) {
        if (this == otherEventType) {
            return null;
        }

        if (!(otherEventType instanceof WrapperEventType)) {
            return new ExprValidationException("Expected a wrapper event type but received " + otherEventType);
        }

        WrapperEventType other = (WrapperEventType) otherEventType;
        ExprValidationException underlyingMapCompare = other.underlyingMapType.equalsCompareType(this.underlyingMapType);
        if (underlyingMapCompare != null) {
            return underlyingMapCompare;
        }

        if (!(other.underlyingEventType instanceof EventTypeSPI) || (!(this.underlyingEventType instanceof EventTypeSPI))) {
            if (!other.underlyingEventType.equals(this.underlyingEventType)) {
                return new ExprValidationException("Wrapper underlying type mismatches");
            }
            return null;
        }

        EventTypeSPI otherUnderlying = (EventTypeSPI) other.underlyingEventType;
        EventTypeSPI thisUnderlying = (EventTypeSPI) this.underlyingEventType;
        return otherUnderlying.equalsCompareType(thisUnderlying);
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        checkInitProperties();
        return propertyDesc;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        checkInitProperties();
        return propertyDescriptorMap.get(propertyName);
    }

    public FragmentEventType getFragmentType(String property) {
        FragmentEventType fragment = underlyingEventType.getFragmentType(property);
        if (fragment != null) {
            return fragment;
        }
        return underlyingMapType.getFragmentType(property);
    }

    public EventPropertyWriterSPI getWriter(String propertyName) {
        if (writableProperties == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, EventPropertyWriterSPI> pair = writers.get(propertyName);
        if (pair == null) {
            return null;
        }
        return pair.getSecond();
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        if (writableProperties == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, EventPropertyWriterSPI> pair = writers.get(propertyName);
        if (pair == null) {
            return null;
        }
        return pair.getFirst();
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        if (writableProperties == null) {
            initializeWriters();
        }
        return writableProperties;
    }

    private void initializeWriters() {
        List<EventPropertyDescriptor> writables = new ArrayList<EventPropertyDescriptor>();
        Map<String, Pair<EventPropertyDescriptor, EventPropertyWriterSPI>> writerMap = new HashMap<String, Pair<EventPropertyDescriptor, EventPropertyWriterSPI>>();
        writables.addAll(Arrays.asList(underlyingMapType.getWriteableProperties()));

        for (EventPropertyDescriptor writableMapProp : underlyingMapType.getWriteableProperties()) {
            final String propertyName = writableMapProp.getPropertyName();
            writables.add(writableMapProp);
            EventPropertyWriterSPI writer = new EventPropertyWriterSPI() {
                public void write(Object value, EventBean target) {
                    DecoratingEventBean decorated = (DecoratingEventBean) target;
                    decorated.getDecoratingProperties().put(propertyName, value);
                }

                public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
                    CodegenExpression decorated = cast(DecoratingEventBean.class, target);
                    CodegenExpression decoratingProps = exprDotMethod(decorated, "getDecoratingProperties");
                    return exprDotMethod(decoratingProps, "put", constant(propertyName), assigned);
                }
            };
            writerMap.put(propertyName, new Pair<>(writableMapProp, writer));
        }

        if (underlyingEventType instanceof EventTypeSPI) {
            EventTypeSPI spi = (EventTypeSPI) underlyingEventType;
            for (EventPropertyDescriptor writableUndProp : spi.getWriteableProperties()) {
                final String propertyName = writableUndProp.getPropertyName();
                final EventPropertyWriter innerWriter = spi.getWriter(propertyName);
                if (innerWriter == null) {
                    continue;
                }

                writables.add(writableUndProp);
                EventPropertyWriterSPI writer = new EventPropertyWriterSPI() {
                    public void write(Object value, EventBean target) {
                        DecoratingEventBean decorated = (DecoratingEventBean) target;
                        innerWriter.write(value, decorated.getUnderlyingEvent());
                    }

                    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
                        CodegenExpression decorated = cast(DecoratingEventBean.class, target);
                        CodegenExpression underlyingBean = exprDotMethod(decorated, "getUnderlyingEvent");
                        CodegenExpression underlying = exprDotMethod(underlyingBean, "getUnderlying");
                        CodegenExpression casted = cast(underlyingEventType.getUnderlyingType(), underlying);
                        return ((EventPropertyWriterSPI) innerWriter).writeCodegen(assigned, casted, target, parent, classScope);
                    }
                };
                writerMap.put(propertyName, new Pair<>(writableUndProp, writer));
            }
        }

        writers = writerMap;
        writableProperties = writables.toArray(new EventPropertyDescriptor[writables.size()]);
    }

    public EventBeanCopyMethodForge getCopyMethodForge(String[] properties) {
        if (writableProperties == null) {
            initializeWriters();
        }

        boolean isOnlyMap = true;
        for (int i = 0; i < properties.length; i++) {
            if (underlyingMapType.getWritableProperty(properties[i]) == null) {
                isOnlyMap = false;
            }
        }

        boolean isOnlyUnderlying = true;
        if (!isOnlyMap) {
            if (!(underlyingEventType instanceof EventTypeSPI)) {
                return null;
            }
            EventTypeSPI spi = (EventTypeSPI) underlyingEventType;
            for (int i = 0; i < properties.length; i++) {
                if (spi.getWritableProperty(properties[i]) == null) {
                    isOnlyUnderlying = false;
                }
            }
        }

        if (isOnlyMap) {
            return new WrapperEventBeanMapCopyMethodForge(this);
        }

        EventBeanCopyMethodForge undCopyMethod = ((EventTypeSPI) underlyingEventType).getCopyMethodForge(properties);
        if (undCopyMethod == null) {
            return null;
        }
        if (isOnlyUnderlying) {
            return new WrapperEventBeanUndCopyMethodForge(this, undCopyMethod);
        } else {
            return new WrapperEventBeanCopyMethodForge(this, undCopyMethod);
        }
    }

    public EventBeanWriter getWriter(String[] properties) {
        if (writableProperties == null) {
            initializeWriters();
        }

        boolean isOnlyMap = true;
        for (int i = 0; i < properties.length; i++) {
            if (!writers.containsKey(properties[i])) {
                return null;
            }
            if (underlyingMapType.getWritableProperty(properties[i]) == null) {
                isOnlyMap = false;
            }
        }

        boolean isOnlyUnderlying = true;
        if (!isOnlyMap) {
            EventTypeSPI spi = (EventTypeSPI) underlyingEventType;
            for (int i = 0; i < properties.length; i++) {
                if (spi.getWritableProperty(properties[i]) == null) {
                    isOnlyUnderlying = false;
                }
            }
        }

        if (isOnlyMap) {
            return new WrapperEventBeanMapWriter(properties);
        }
        if (isOnlyUnderlying) {
            EventTypeSPI spi = (EventTypeSPI) underlyingEventType;
            EventBeanWriter undWriter = spi.getWriter(properties);
            if (undWriter == null) {
                return undWriter;
            }
            return new WrapperEventBeanUndWriter(undWriter);
        }

        EventPropertyWriter[] writerArr = new EventPropertyWriter[properties.length];
        for (int i = 0; i < properties.length; i++) {
            writerArr[i] = writers.get(properties[i]).getSecond();
        }
        return new WrapperEventBeanPropertyWriter(writerArr);
    }

    private void checkForRepeatedPropertyNames(EventType eventType, Map<String, Object> properties) {
        for (String property : eventType.getPropertyNames()) {
            if (properties.keySet().contains(property)) {
                throw new EPException("Property '" + property + "' occurs in both the underlying event and in the additional properties");
            }
        }
    }

    public EventTypeIdPair computeIdFromWrapped(NameAccessModifier visibility, String innerName, EventTypeMetadata metadataWrapper) {
        if (visibility == TRANSIENT || visibility == PRIVATE) {
            return new EventTypeIdPair(metadataWrapper.getEventTypeIdPair().getPublicId(), CRC32Util.computeCRC32(innerName));
        }
        return new EventTypeIdPair(CRC32Util.computeCRC32(innerName), -1);
    }

    public static class PropertyDescriptorComposite {
        private final HashMap<String, EventPropertyDescriptor> propertyDescriptorMap;
        private final String[] propertyNames;
        private final EventPropertyDescriptor[] descriptors;

        public PropertyDescriptorComposite(HashMap<String, EventPropertyDescriptor> propertyDescriptorMap, String[] propertyNames, EventPropertyDescriptor[] descriptors) {
            this.propertyDescriptorMap = propertyDescriptorMap;
            this.propertyNames = propertyNames;
            this.descriptors = descriptors;
        }

        public HashMap<String, EventPropertyDescriptor> getPropertyDescriptorMap() {
            return propertyDescriptorMap;
        }

        public String[] getPropertyNames() {
            return propertyNames;
        }

        public EventPropertyDescriptor[] getDescriptors() {
            return descriptors;
        }
    }
}
