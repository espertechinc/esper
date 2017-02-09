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
package com.espertech.esper.event;

import com.espertech.esper.client.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.event.map.MapEventType;

import java.util.*;

/**
 * An event type that adds zero or more fields to an existing event type.
 * <p>
 * The additional fields are represented as a Map. Any queries to event properties are first
 * held against the additional fields, and secondly are handed through to the underlying event.
 * <p>
 * If this event type is to add information to another wrapper event type (wrapper to wrapper), then it is the
 * responsibility of the creating logic to use the existing event type and add to it.
 * <p>
 * Uses a the map event type {@link com.espertech.esper.event.map.MapEventType} to represent the mapped properties. This is because the additional properties
 * can also be beans or complex types and the Map event type handles these nicely.
 */
public class WrapperEventType implements EventTypeSPI {
    /**
     * event type metadata
     */
    protected final EventTypeMetadata metadata;

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
    private final int eventTypeId;

    private final boolean isNoMapProperties;
    private final Map<String, EventPropertyGetter> propertyGetterCache;
    private final EventAdapterService eventAdapterService;
    private EventPropertyDescriptor[] writableProperties;
    private Map<String, Pair<EventPropertyDescriptor, EventPropertyWriter>> writers;

    private String startTimestampPropertyName;
    private String endTimestampPropertyName;
    private int numPropertiesUnderlyingType;

    /**
     * Ctor.
     *
     * @param typeName            is the event type name
     * @param eventType           is the event type of the wrapped events
     * @param properties          is the additional properties this wrapper adds
     * @param metadata            event type metadata
     * @param eventAdapterService is the service for resolving unknown wrapped types
     * @param eventTypeId         type id
     */
    public WrapperEventType(EventTypeMetadata metadata, String typeName, int eventTypeId, EventType eventType, Map<String, Object> properties, EventAdapterService eventAdapterService) {
        checkForRepeatedPropertyNames(eventType, properties);

        this.metadata = metadata;
        this.underlyingEventType = eventType;
        EventTypeMetadata metadataMapType = EventTypeMetadata.createAnonymous(typeName, EventTypeMetadata.ApplicationType.MAP);
        this.underlyingMapType = new MapEventType(metadataMapType, typeName, 0, eventAdapterService, properties, null, null, null);
        this.isNoMapProperties = properties.isEmpty();
        this.eventAdapterService = eventAdapterService;
        this.eventTypeId = eventTypeId;
        propertyGetterCache = new HashMap<String, EventPropertyGetter>();

        updatePropertySet();

        if (metadata.getTypeClass() == EventTypeMetadata.TypeClass.NAMED_WINDOW) {
            startTimestampPropertyName = eventType.getStartTimestampPropertyName();
            endTimestampPropertyName = eventType.getEndTimestampPropertyName();
            EventTypeUtility.validateTimestampProperties(this, startTimestampPropertyName, endTimestampPropertyName);
        }
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

    public String getName() {
        return metadata.getPublicName();
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    public EventPropertyGetter getGetter(final String property) {
        EventPropertyGetter cachedGetter = propertyGetterCache.get(property);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        if (underlyingMapType.isProperty(property) && (property.indexOf('?') == -1)) {
            final EventPropertyGetter mapGetter = underlyingMapType.getGetter(property);
            EventPropertyGetter getter = new EventPropertyGetter() {
                public Object get(EventBean theEvent) {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    return mapGetter.get(eventAdapterService.adapterForTypedMap(map, underlyingMapType));
                }

                public boolean isExistsProperty(EventBean eventBean) {
                    return true; // Property exists as the property is not dynamic (unchecked)
                }

                public Object getFragment(EventBean theEvent) {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    return mapGetter.getFragment(eventAdapterService.adapterForTypedMap(map, underlyingMapType));
                }
            };
            propertyGetterCache.put(property, getter);
            return getter;
        } else if (underlyingEventType.isProperty(property)) {
            EventPropertyGetter getter = new EventPropertyGetter() {
                public Object get(EventBean theEvent) {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
                    if (wrappedEvent == null) {
                        return null;
                    }

                    EventPropertyGetter underlyingGetter = underlyingEventType.getGetter(property);
                    return underlyingGetter.get(wrappedEvent);
                }

                public boolean isExistsProperty(EventBean eventBean) {
                    return true; // Property exists as the property is not dynamic (unchecked)
                }

                public Object getFragment(EventBean theEvent) {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
                    if (wrappedEvent == null) {
                        return null;
                    }

                    EventPropertyGetter underlyingGetter = underlyingEventType.getGetter(property);
                    return underlyingGetter.getFragment(wrappedEvent);
                }
            };
            propertyGetterCache.put(property, getter);
            return getter;
        } else {
            return null;
        }
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedProperty) {
        final EventPropertyGetterMapped undMapped = underlyingEventType.getGetterMapped(mappedProperty);
        if (undMapped != null) {
            return new EventPropertyGetterMapped() {
                public Object get(EventBean theEvent, String mapKey) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
                    if (wrappedEvent == null) {
                        return null;
                    }
                    return undMapped.get(wrappedEvent, mapKey);
                }
            };
        }
        final EventPropertyGetterMapped decoMapped = underlyingMapType.getGetterMapped(mappedProperty);
        if (decoMapped != null) {
            return new EventPropertyGetterMapped() {
                public Object get(EventBean theEvent, String mapKey) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    return decoMapped.get(eventAdapterService.adapterForTypedMap(map, underlyingMapType), mapKey);
                }
            };
        }
        return null;
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedProperty) {
        final EventPropertyGetterIndexed undIndexed = underlyingEventType.getGetterIndexed(indexedProperty);
        if (undIndexed != null) {
            return new EventPropertyGetterIndexed() {
                public Object get(EventBean theEvent, int index) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
                    if (wrappedEvent == null) {
                        return null;
                    }
                    return undIndexed.get(wrappedEvent, index);
                }
            };
        }
        final EventPropertyGetterIndexed decoIndexed = underlyingMapType.getGetterIndexed(indexedProperty);
        if (decoIndexed != null) {
            return new EventPropertyGetterIndexed() {
                public Object get(EventBean theEvent, int index) throws PropertyAccessException {
                    if (!(theEvent instanceof DecoratingEventBean)) {
                        throw new PropertyAccessException("Mismatched property getter to EventBean type");
                    }
                    DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
                    Map map = wrapperEvent.getDecoratingProperties();
                    return decoIndexed.get(eventAdapterService.adapterForTypedMap(map, underlyingMapType), index);
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

    public EventBeanReader getReader() {
        return null;
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
                "underlyingEventType=" + underlyingEventType + " " +
                "underlyingMapType=" + underlyingMapType;
    }

    public boolean equalsCompareType(EventType otherEventType) {
        if (this == otherEventType) {
            return true;
        }

        if (!(otherEventType instanceof WrapperEventType)) {
            return false;
        }

        WrapperEventType other = (WrapperEventType) otherEventType;
        if (!other.underlyingMapType.equalsCompareType(this.underlyingMapType)) {
            return false;
        }

        if (!(other.underlyingEventType instanceof EventTypeSPI) || (!(this.underlyingEventType instanceof EventTypeSPI))) {
            return other.underlyingEventType.equals(this.underlyingEventType);
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

    public EventPropertyWriter getWriter(String propertyName) {
        if (writableProperties == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, EventPropertyWriter> pair = writers.get(propertyName);
        if (pair == null) {
            return null;
        }
        return pair.getSecond();
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        if (writableProperties == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, EventPropertyWriter> pair = writers.get(propertyName);
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
        Map<String, Pair<EventPropertyDescriptor, EventPropertyWriter>> writerMap = new HashMap<String, Pair<EventPropertyDescriptor, EventPropertyWriter>>();
        writables.addAll(Arrays.asList(underlyingMapType.getWriteableProperties()));

        for (EventPropertyDescriptor writableMapProp : underlyingMapType.getWriteableProperties()) {
            final String propertyName = writableMapProp.getPropertyName();
            writables.add(writableMapProp);
            EventPropertyWriter writer = new EventPropertyWriter() {
                public void write(Object value, EventBean target) {
                    DecoratingEventBean decorated = (DecoratingEventBean) target;
                    decorated.getDecoratingProperties().put(propertyName, value);
                }
            };
            writerMap.put(propertyName, new Pair<EventPropertyDescriptor, EventPropertyWriter>(writableMapProp, writer));
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
                EventPropertyWriter writer = new EventPropertyWriter() {
                    public void write(Object value, EventBean target) {
                        DecoratingEventBean decorated = (DecoratingEventBean) target;
                        innerWriter.write(value, decorated.getUnderlyingEvent());
                    }
                };
                writerMap.put(propertyName, new Pair<EventPropertyDescriptor, EventPropertyWriter>(writableUndProp, writer));
            }
        }

        writers = writerMap;
        writableProperties = writables.toArray(new EventPropertyDescriptor[writables.size()]);
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
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
            return new WrapperEventBeanMapCopyMethod(this, eventAdapterService);
        }

        EventBeanCopyMethod undCopyMethod = ((EventTypeSPI) underlyingEventType).getCopyMethod(properties);
        if (undCopyMethod == null) {
            return null;
        }
        if (isOnlyUnderlying) {
            return new WrapperEventBeanUndCopyMethod(this, eventAdapterService, undCopyMethod);
        } else {
            return new WrapperEventBeanCopyMethod(this, eventAdapterService, undCopyMethod);
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
                throw new EPException("Property " + property + " occurs in both the underlying event and in the additional properties");
            }
        }
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
