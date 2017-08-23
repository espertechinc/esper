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
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.event.bean.BeanEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.EventBeanManufacturerBean;
import com.espertech.esper.event.bean.PropertyHelper;
import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.xml.BaseXMLEventType;
import com.espertech.esper.event.xml.XMLEventBean;
import com.espertech.esper.util.JavaClassHelper;
import org.w3c.dom.Node;

import java.util.*;

/**
 * Helper for writeable events.
 */
public class EventAdapterServiceHelper {
    public static String getMessageExpecting(String eventTypeName, EventType existingType, String typeOfEventType) {
        String message = "Event type named '" + eventTypeName + "' has not been defined or is not a " + typeOfEventType + " event type";
        if (existingType != null) {
            message += ", the name '" + eventTypeName + "' refers to a " + JavaClassHelper.getClassNameFullyQualPretty(existingType.getUnderlyingType()) + " event type";
        } else {
            message += ", the name '" + eventTypeName + "' has not been defined as an event type";
        }
        return message;
    }

    public static EventBeanFactory getFactoryForType(EventType type, EventAdapterService eventAdapterService) {
        if (type instanceof WrapperEventType) {
            WrapperEventType wrapperType = (WrapperEventType) type;
            if (wrapperType.getUnderlyingEventType() instanceof BeanEventType) {
                return new EventBeanFactoryBeanWrapped(wrapperType.getUnderlyingEventType(), wrapperType, eventAdapterService);
            }
        }
        if (type instanceof BeanEventType) {
            return new EventBeanFactoryBean(type, eventAdapterService);
        }
        if (type instanceof MapEventType) {
            return new EventBeanFactoryMap(type, eventAdapterService);
        }
        if (type instanceof ObjectArrayEventType) {
            return new EventBeanFactoryObjectArray(type, eventAdapterService);
        }
        if (type instanceof BaseXMLEventType) {
            return new EventBeanFactoryXML(type, eventAdapterService);
        }
        if (type instanceof AvroSchemaEventType) {
            return eventAdapterService.getEventAdapterAvroHandler().getEventBeanFactory(type, eventAdapterService);
        }
        throw new IllegalArgumentException("Cannot create event bean factory for event type '" + type.getName() + "': " + type.getClass().getName() + " is not a recognized event type or supported wrap event type");
    }

    /**
     * Returns descriptors for all writable properties.
     *
     * @param eventType    to reflect on
     * @param allowAnyType whether any type property can be populated
     * @return list of writable properties
     */
    public static Set<WriteablePropertyDescriptor> getWriteableProperties(EventType eventType, boolean allowAnyType) {
        if (!(eventType instanceof EventTypeSPI)) {
            return null;
        }
        if (eventType instanceof BeanEventType) {
            BeanEventType beanEventType = (BeanEventType) eventType;
            return PropertyHelper.getWritableProperties(beanEventType.getUnderlyingType());
        }
        EventTypeSPI typeSPI = (EventTypeSPI) eventType;
        if (!allowAnyType && !allowPropulate(typeSPI)) {
            return null;
        }
        if (eventType instanceof BaseNestableEventType) {
            Map<String, Object> mapdef = ((BaseNestableEventType) eventType).getTypes();
            Set<WriteablePropertyDescriptor> writables = new LinkedHashSet<WriteablePropertyDescriptor>();
            for (Map.Entry<String, Object> types : mapdef.entrySet()) {
                if (types.getValue() instanceof Class) {
                    writables.add(new WriteablePropertyDescriptor(types.getKey(), (Class) types.getValue(), null));
                }
                if (types.getValue() instanceof String) {
                    String typeName = types.getValue().toString();
                    Class clazz = JavaClassHelper.getPrimitiveClassForName(typeName);
                    if (clazz != null) {
                        writables.add(new WriteablePropertyDescriptor(types.getKey(), clazz, null));
                    }
                }
            }
            return writables;
        } else if (eventType instanceof AvroSchemaEventType) {
            Set<WriteablePropertyDescriptor> writables = new LinkedHashSet<WriteablePropertyDescriptor>();
            EventPropertyDescriptor[] desc = typeSPI.getWriteableProperties();
            for (EventPropertyDescriptor prop : desc) {
                writables.add(new WriteablePropertyDescriptor(prop.getPropertyName(), prop.getPropertyType(), null));
            }
            return writables;
        } else {
            return null;
        }
    }

    private static boolean allowPropulate(EventTypeSPI typeSPI) {
        if (!typeSPI.getMetadata().isApplicationConfigured() &&
                typeSPI.getMetadata().getTypeClass() != EventTypeMetadata.TypeClass.ANONYMOUS &&
                typeSPI.getMetadata().getTypeClass() != EventTypeMetadata.TypeClass.TABLE) {
            return false;
        }
        return true;
    }

    /**
     * Return an adapter for the given type of event using the pre-validated object.
     *
     * @param theEvent            value object
     * @param eventType           type of event
     * @param eventAdapterService service for instances
     * @return event adapter
     */
    public static EventBean adapterForType(Object theEvent, EventType eventType, EventAdapterService eventAdapterService) {
        if (theEvent == null) {
            return null;
        }
        if (eventType instanceof BeanEventType) {
            return eventAdapterService.adapterForTypedBean(theEvent, (BeanEventType) eventType);
        } else if (eventType instanceof MapEventType) {
            return eventAdapterService.adapterForTypedMap((Map) theEvent, eventType);
        } else if (eventType instanceof ObjectArrayEventType) {
            return eventAdapterService.adapterForTypedObjectArray((Object[]) theEvent, eventType);
        } else if (eventType instanceof BaseConfigurableEventType) {
            return eventAdapterService.adapterForTypedDOM((Node) theEvent, eventType);
        } else if (eventType instanceof AvroSchemaEventType) {
            return eventAdapterService.adapterForTypedAvro(theEvent, eventType);
        } else {
            return null;
        }
    }

    /**
     * Returns a factory for creating and populating event object instances for the given type.
     *
     * @param eventAdapterService fatory for event
     * @param eventType           to create underlying objects for
     * @param properties          to write
     * @param engineImportService for resolving methods
     * @param allowAnyType        whether any type property can be populated
     * @param avroHandler         avro handler
     * @return factory
     * @throws EventBeanManufactureException if a factory cannot be created for the type
     */
    public static EventBeanManufacturer getManufacturer(EventAdapterService eventAdapterService, EventType eventType, WriteablePropertyDescriptor[] properties, EngineImportService engineImportService, boolean allowAnyType, EventAdapterAvroHandler avroHandler)
            throws EventBeanManufactureException {
        if (!(eventType instanceof EventTypeSPI)) {
            return null;
        }
        if (eventType instanceof BeanEventType) {
            BeanEventType beanEventType = (BeanEventType) eventType;
            return new EventBeanManufacturerBean(beanEventType, eventAdapterService, properties, engineImportService);
        }
        EventTypeSPI typeSPI = (EventTypeSPI) eventType;
        if (!allowAnyType && !allowPropulate(typeSPI)) {
            return null;
        }
        if (eventType instanceof MapEventType) {
            MapEventType mapEventType = (MapEventType) eventType;
            return new EventBeanManufacturerMap(mapEventType, eventAdapterService, properties);
        }
        if (eventType instanceof ObjectArrayEventType) {
            ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) eventType;
            return new EventBeanManufacturerObjectArray(objectArrayEventType, eventAdapterService, properties);
        }
        if (eventType instanceof AvroSchemaEventType) {
            AvroSchemaEventType avroSchemaEventType = (AvroSchemaEventType) eventType;
            return avroHandler.getEventBeanManufacturer(avroSchemaEventType, eventAdapterService, properties);
        }
        return null;
    }

    public static EventBean[] typeCast(List<EventBean> events, EventType targetType, EventAdapterService eventAdapterService) {
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
                Object convertedGenericRecord = eventAdapterService.getEventAdapterAvroHandler().convertEvent(theEvent, (AvroSchemaEventType) targetType);
                converted = eventAdapterService.adapterForTypedAvro(convertedGenericRecord, targetType);
            } else {
                throw new EPException("Unknown event type " + theEvent.getEventType());
            }
            convertedArray[count] = converted;
            count++;
        }
        return convertedArray;
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
        throw new EventAdapterException("Event type '" + eventType.getName() + "' is not an engine-native event type");
    }

    public static EventBeanAdapterFactory getAdapterFactoryForType(EventType eventType) {
        if (eventType instanceof BeanEventType) {
            return new EventBeanAdapterFactoryBean(eventType);
        }
        if (eventType instanceof ObjectArrayEventType) {
            return new EventBeanAdapterFactoryObjectArray(eventType);
        }
        if (eventType instanceof MapEventType) {
            return new EventBeanAdapterFactoryMap(eventType);
        }
        if (eventType instanceof BaseXMLEventType) {
            return new EventBeanAdapterFactoryXml(eventType);
        }
        throw new EventAdapterException("Event type '" + eventType.getName() + "' is not an engine-native event type");
    }

    public static class EventBeanAdapterFactoryBean implements EventBeanAdapterFactory {
        private final EventType eventType;

        public EventBeanAdapterFactoryBean(EventType eventType) {
            this.eventType = eventType;
        }

        public EventBean makeAdapter(Object underlying) {
            return new BeanEventBean(underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryMap implements EventBeanAdapterFactory {
        private final EventType eventType;

        public EventBeanAdapterFactoryMap(EventType eventType) {
            this.eventType = eventType;
        }

        public EventBean makeAdapter(Object underlying) {
            return new MapEventBean((Map<String, Object>) underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryObjectArray implements EventBeanAdapterFactory {
        private final EventType eventType;

        public EventBeanAdapterFactoryObjectArray(EventType eventType) {
            this.eventType = eventType;
        }

        public EventBean makeAdapter(Object underlying) {
            return new ObjectArrayEventBean((Object[]) underlying, eventType);
        }
    }

    public static class EventBeanAdapterFactoryXml implements EventBeanAdapterFactory {
        private final EventType eventType;

        public EventBeanAdapterFactoryXml(EventType eventType) {
            this.eventType = eventType;
        }

        public EventBean makeAdapter(Object underlying) {
            return new XMLEventBean((Node) underlying, eventType);
        }
    }
}
