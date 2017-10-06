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
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.event.bean.BeanEventAdapter;
import com.espertech.esper.event.bean.BeanEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.BeanEventTypeFactory;
import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.xml.*;
import com.espertech.esper.plugin.*;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for resolving event name to event type.
 * <p>
 * The implementation assigned a unique identifier to each event type.
 * For Class-based event types, only one EventType instance and one event type id exists for the same class.
 * <p>
 * Event type names must be unique, that is an name must resolve to a single event type.
 * <p>
 * Each event type can have multiple names defined for it. For example, expressions such as
 * "select * from A" and "select * from B"
 * in which A and B are names for the same class X the select clauses each fireStatementStopped for events of type X.
 * In summary, names A and B point to the same underlying event type and therefore event type id.
 */
public class EventAdapterServiceImpl implements EventAdapterService {
    private final static Logger log = LoggerFactory.getLogger(EventAdapterServiceImpl.class);

    private final ConcurrentHashMap<Class, BeanEventType> typesPerJavaBean;
    private final Map<String, EventType> nameToTypeMap;
    private final Map<String, PlugInEventTypeHandler> nameToHandlerMap;
    private BeanEventAdapter beanEventAdapter;
    private Map<String, EventType> xmldomRootElementNames;
    private LinkedHashSet<String> javaPackageNames;
    private final Map<URI, PlugInEventRepresentation> plugInRepresentations;
    private final EventTypeIdGenerator eventTypeIdGenerator;
    private final EventAdapterServiceAnonymousTypeCache anonymousTypeCache;
    private final EventAdapterAvroHandler avroHandler;
    private final EngineImportService engineImportService;

    public EventAdapterServiceImpl(EventTypeIdGenerator eventTypeIdGenerator,
                                   int anonymousTypeCacheSize,
                                   EventAdapterAvroHandler avroHandler,
                                   EngineImportService engineImportService) {
        this.eventTypeIdGenerator = eventTypeIdGenerator;
        this.avroHandler = avroHandler;
        this.engineImportService = engineImportService;

        nameToTypeMap = new HashMap<String, EventType>();
        xmldomRootElementNames = new HashMap<String, EventType>();
        javaPackageNames = new LinkedHashSet<String>();
        nameToHandlerMap = new HashMap<String, PlugInEventTypeHandler>();

        // Share the mapping of class to type with the type creation for thread safety
        typesPerJavaBean = new ConcurrentHashMap<Class, BeanEventType>();
        beanEventAdapter = new BeanEventAdapter(typesPerJavaBean, this, eventTypeIdGenerator);
        plugInRepresentations = new HashMap<URI, PlugInEventRepresentation>();
        anonymousTypeCache = new EventAdapterServiceAnonymousTypeCache(anonymousTypeCacheSize);
    }

    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    public Map<String, EventType> getDeclaredEventTypes() {
        return new HashMap<String, EventType>(nameToTypeMap);
    }

    /**
     * Set the legacy Java class type information.
     *
     * @param classToLegacyConfigs is the legacy class configs
     */
    public void setClassLegacyConfigs(Map<String, ConfigurationEventTypeLegacy> classToLegacyConfigs) {
        beanEventAdapter.setClassToLegacyConfigs(classToLegacyConfigs);
    }

    public ConfigurationEventTypeLegacy getClassLegacyConfigs(String className) {
        return beanEventAdapter.getClassToLegacyConfigs(className);
    }

    public Set<WriteablePropertyDescriptor> getWriteableProperties(EventType eventType, boolean allowAnyType) {
        return EventAdapterServiceHelper.getWriteableProperties(eventType, allowAnyType);
    }

    public EventBeanManufacturer getManufacturer(EventType eventType, WriteablePropertyDescriptor[] properties, EngineImportService engineImportService, boolean allowAnyType)
            throws EventBeanManufactureException {
        return EventAdapterServiceHelper.getManufacturer(this, eventType, properties, engineImportService, allowAnyType, avroHandler);
    }

    public EventType[] getAllTypes() {
        Collection<EventType> types = nameToTypeMap.values();
        return types.toArray(new EventType[types.size()]);
    }

    public synchronized void addTypeByName(String name, EventType eventType) throws EventAdapterException {
        if (nameToTypeMap.containsKey(name)) {
            throw new EventAdapterException("Event type by name '" + name + "' already exists");
        }
        nameToTypeMap.put(name, eventType);
    }

    public void addEventRepresentation(URI eventRepURI, PlugInEventRepresentation pluginEventRep) throws EventAdapterException {
        if (plugInRepresentations.containsKey(eventRepURI)) {
            throw new EventAdapterException("Plug-in event representation URI by name " + eventRepURI + " already exists");
        }
        plugInRepresentations.put(eventRepURI, pluginEventRep);
    }

    public EventType addPlugInEventType(String eventTypeName, URI[] resolutionURIs, Serializable initializer) throws EventAdapterException {
        if (nameToTypeMap.containsKey(eventTypeName)) {
            throw new EventAdapterException("Event type named '" + eventTypeName +
                    "' has already been declared");
        }

        PlugInEventRepresentation handlingFactory = null;
        URI handledEventTypeURI = null;

        if ((resolutionURIs == null) || (resolutionURIs.length == 0)) {
            throw new EventAdapterException("Event type named '" + eventTypeName + "' could not be created as" +
                    " no resolution URIs for dynamic resolution of event type names through a plug-in event representation have been defined");
        }

        for (URI eventTypeURI : resolutionURIs) {
            // Determine a list of event representations that may handle this type
            Map<URI, Object> allFactories = new HashMap<URI, Object>(plugInRepresentations);
            Collection<Map.Entry<URI, Object>> factories = URIUtil.filterSort(eventTypeURI, allFactories);

            if (factories.isEmpty()) {
                continue;
            }

            // Ask each in turn to accept the type (the process of resolving the type)
            for (Map.Entry<URI, Object> entry : factories) {
                PlugInEventRepresentation factory = (PlugInEventRepresentation) entry.getValue();
                PlugInEventTypeHandlerContext context = new PlugInEventTypeHandlerContext(eventTypeURI, initializer, eventTypeName, eventTypeIdGenerator.getTypeId(eventTypeName));
                if (factory.acceptsType(context)) {
                    handlingFactory = factory;
                    handledEventTypeURI = eventTypeURI;
                    break;
                }
            }

            if (handlingFactory != null) {
                break;
            }
        }

        if (handlingFactory == null) {
            throw new EventAdapterException("Event type named '" + eventTypeName + "' could not be created as none of the " +
                    "registered plug-in event representations accepts any of the resolution URIs '" + Arrays.toString(resolutionURIs)
                    + "' and initializer");
        }

        PlugInEventTypeHandlerContext context = new PlugInEventTypeHandlerContext(handledEventTypeURI, initializer, eventTypeName, eventTypeIdGenerator.getTypeId(eventTypeName));
        PlugInEventTypeHandler handler = handlingFactory.getTypeHandler(context);
        if (handler == null) {
            throw new EventAdapterException("Event type named '" + eventTypeName + "' could not be created as no handler was returned");
        }

        EventType eventType = handler.getType();
        nameToTypeMap.put(eventTypeName, eventType);
        nameToHandlerMap.put(eventTypeName, handler);

        return eventType;
    }

    public EventSender getStaticTypeEventSender(EPRuntimeEventSender runtimeEventSender, String eventTypeName, ThreadingService threadingService) throws EventTypeException {
        EventType eventType = nameToTypeMap.get(eventTypeName);
        if (eventType == null) {
            throw new EventTypeException("Event type named '" + eventTypeName + "' could not be found");
        }

        // handle built-in types
        if (eventType instanceof BeanEventType) {
            return new EventSenderBean(runtimeEventSender, (BeanEventType) eventType, this, threadingService);
        }
        if (eventType instanceof MapEventType) {
            return new EventSenderMap(runtimeEventSender, (MapEventType) eventType, this, threadingService);
        }
        if (eventType instanceof ObjectArrayEventType) {
            return new EventSenderObjectArray(runtimeEventSender, (ObjectArrayEventType) eventType, this, threadingService);
        }
        if (eventType instanceof BaseXMLEventType) {
            return new EventSenderXMLDOM(runtimeEventSender, (BaseXMLEventType) eventType, this, threadingService);
        }
        if (eventType instanceof AvroSchemaEventType) {
            return new EventSenderAvro(runtimeEventSender, eventType, this, threadingService);
        }

        PlugInEventTypeHandler handlers = nameToHandlerMap.get(eventTypeName);
        if (handlers != null) {
            return handlers.getSender(runtimeEventSender);
        }
        throw new EventTypeException("An event sender for event type named '" + eventTypeName + "' could not be created as the type is internal");
    }

    public void updateMapEventType(String mapeventTypeName, Map<String, Object> typeMap) throws EventAdapterException {
        EventType type = nameToTypeMap.get(mapeventTypeName);
        if (type == null) {
            throw new EventAdapterException("Event type named '" + mapeventTypeName + "' has not been declared");
        }
        if (!(type instanceof MapEventType)) {
            throw new EventAdapterException("Event type by name '" + mapeventTypeName + "' is not a Map event type");
        }

        MapEventType mapEventType = (MapEventType) type;
        mapEventType.addAdditionalProperties(typeMap, this);
    }

    public void updateObjectArrayEventType(String objectArrayEventTypeName, Map<String, Object> typeMap) throws EventAdapterException {
        EventType type = nameToTypeMap.get(objectArrayEventTypeName);
        if (type == null) {
            throw new EventAdapterException("Event type named '" + objectArrayEventTypeName + "' has not been declared");
        }
        if (!(type instanceof ObjectArrayEventType)) {
            throw new EventAdapterException("Event type by name '" + objectArrayEventTypeName + "' is not an Object-array event type");
        }

        ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) type;
        objectArrayEventType.addAdditionalProperties(typeMap, this);
    }

    public EventSender getDynamicTypeEventSender(EPRuntimeEventSender epRuntime, URI[] uri, ThreadingService threadingService) throws EventTypeException {
        List<EventSenderURIDesc> handlingFactories = new ArrayList<EventSenderURIDesc>();
        for (URI resolutionURI : uri) {
            // Determine a list of event representations that may handle this type
            Map<URI, Object> allFactories = new HashMap<URI, Object>(plugInRepresentations);
            Collection<Map.Entry<URI, Object>> factories = URIUtil.filterSort(resolutionURI, allFactories);

            if (factories.isEmpty()) {
                continue;
            }

            // Ask each in turn to accept the type (the process of resolving the type)
            for (Map.Entry<URI, Object> entry : factories) {
                PlugInEventRepresentation factory = (PlugInEventRepresentation) entry.getValue();
                PlugInEventBeanReflectorContext context = new PlugInEventBeanReflectorContext(resolutionURI);
                if (factory.acceptsEventBeanResolution(context)) {
                    PlugInEventBeanFactory beanFactory = factory.getEventBeanFactory(context);
                    if (beanFactory == null) {
                        log.warn("Plug-in event representation returned a null bean factory, ignoring entry");
                        continue;
                    }
                    EventSenderURIDesc desc = new EventSenderURIDesc(beanFactory, resolutionURI, entry.getKey());
                    handlingFactories.add(desc);
                }
            }
        }

        if (handlingFactories.isEmpty()) {
            throw new EventTypeException("Event sender for resolution URIs '" + Arrays.toString(uri)
                    + "' did not return at least one event representation's event factory");
        }

        return new EventSenderImpl(handlingFactories, epRuntime, threadingService);
    }

    public BeanEventTypeFactory getBeanEventTypeFactory() {
        return beanEventAdapter;
    }

    /**
     * Sets the default property resolution style.
     *
     * @param defaultPropertyResolutionStyle is the default style
     */
    public void setDefaultPropertyResolutionStyle(Configuration.PropertyResolutionStyle defaultPropertyResolutionStyle) {
        beanEventAdapter.setDefaultPropertyResolutionStyle(defaultPropertyResolutionStyle);
    }

    public void setDefaultAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle defaultAccessorStyle) {
        beanEventAdapter.setDefaultAccessorStyle(defaultAccessorStyle);
    }

    public EventType getExistsTypeByName(String eventTypeName) {
        if (eventTypeName == null) {
            throw new IllegalStateException("Null event type name parameter");
        }
        return nameToTypeMap.get(eventTypeName);
    }

    public synchronized EventType addBeanType(String eventTypeName, Class clazz, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured) throws EventAdapterException {
        if (log.isDebugEnabled()) {
            log.debug(".addBeanType Adding " + eventTypeName + " for type " + clazz.getName());
        }

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            if (existingType.getUnderlyingType().equals(clazz)) {
                return existingType;
            }

            throw new EventAdapterException("Event type named '" + eventTypeName +
                    "' has already been declared with differing underlying type information:" + existingType.getUnderlyingType().getName() +
                    " versus " + clazz.getName());
        }

        EventType eventType = beanEventAdapter.createBeanType(eventTypeName, clazz, isPreconfiguredStatic, isPreconfigured, isConfigured);
        nameToTypeMap.put(eventTypeName, eventType);

        return eventType;
    }

    public synchronized EventType addBeanTypeByName(String eventTypeName, Class clazz, boolean isNamedWindow) throws EventAdapterException {
        if (log.isDebugEnabled()) {
            log.debug(".addBeanTypeNamedWindow Adding " + eventTypeName + " for type " + clazz.getName());
        }

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            if (existingType instanceof BeanEventType &&
                    existingType.getUnderlyingType() == clazz &&
                    existingType.getName().equals(eventTypeName)) {
                EventTypeMetadata.TypeClass typeClass = ((BeanEventType) existingType).getMetadata().getTypeClass();
                if (isNamedWindow) {
                    if (typeClass == EventTypeMetadata.TypeClass.NAMED_WINDOW) {
                        return existingType;
                    }
                } else {
                    if (typeClass == EventTypeMetadata.TypeClass.STREAM) {
                        return existingType;
                    }
                }
            }
            throw new EventAdapterException("An event type named '" + eventTypeName + "' has already been declared");
        }

        EventTypeMetadata.TypeClass typeClass = isNamedWindow ? EventTypeMetadata.TypeClass.NAMED_WINDOW : EventTypeMetadata.TypeClass.STREAM;
        BeanEventType beanEventType = new BeanEventType(EventTypeMetadata.createBeanType(eventTypeName, clazz, false, false, false, typeClass),
                eventTypeIdGenerator.getTypeId(eventTypeName), clazz, this, beanEventAdapter.getClassToLegacyConfigs(clazz.getName()));
        nameToTypeMap.put(eventTypeName, beanEventType);

        return beanEventType;
    }

    /**
     * Create an event bean given an event of object id.
     *
     * @param theEvent is the event class
     * @return event
     */
    public EventBean adapterForBean(Object theEvent) {
        EventType eventType = typesPerJavaBean.get(theEvent.getClass());
        if (eventType == null) {
            // This will update the typesPerJavaBean mapping
            eventType = beanEventAdapter.createBeanType(theEvent.getClass().getName(), theEvent.getClass(), false, false, false);
        }
        return new BeanEventBean(theEvent, eventType);
    }

    /**
     * Add an event type for the given Java class name.
     *
     * @param eventTypeName      is the name
     * @param fullyQualClassName is the Java class name
     * @return event type
     * @throws EventAdapterException if the Class name cannot resolve or other error occured
     */
    public synchronized EventType addBeanType(String eventTypeName, String fullyQualClassName, boolean considerAutoName, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured) throws EventAdapterException {
        if (log.isDebugEnabled()) {
            log.debug(".addBeanType Adding " + eventTypeName + " for type " + fullyQualClassName);
        }

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            if ((existingType.getUnderlyingType().getName().equals(fullyQualClassName)) ||
                    (existingType.getUnderlyingType().getSimpleName().equals(fullyQualClassName))) {
                if (log.isDebugEnabled()) {
                    log.debug(".addBeanType Returning existing type for " + eventTypeName);
                }
                return existingType;
            }

            throw new EventAdapterException("Event type named '" + eventTypeName +
                    "' has already been declared with differing underlying type information: Class " + existingType.getUnderlyingType().getName() +
                    " versus " + fullyQualClassName);
        }

        // Try to resolve as a fully-qualified class name first
        Class clazz = null;
        try {
            clazz = engineImportService.getClassForNameProvider().classForName(fullyQualClassName);
        } catch (ClassNotFoundException ex) {
            if (!considerAutoName) {
                throw new EventAdapterException("Event type or class named '" + fullyQualClassName + "' was not found", ex);
            }

            // Attempt to resolve from auto-name packages
            for (String javaPackageName : javaPackageNames) {
                String generatedClassName = javaPackageName + "." + fullyQualClassName;
                try {
                    Class resolvedClass = engineImportService.getClassForNameProvider().classForName(generatedClassName);
                    if (clazz != null) {
                        throw new EventAdapterException("Failed to resolve name '" + eventTypeName + "', the class was ambigously found both in " +
                                "package '" + clazz.getPackage().getName() + "' and in " +
                                "package '" + resolvedClass.getPackage().getName() + "'", ex);
                    }
                    clazz = resolvedClass;
                } catch (ClassNotFoundException ex1) {
                    // expected, class may not exists in all packages
                }
            }
            if (clazz == null) {
                throw new EventAdapterException("Event type or class named '" + fullyQualClassName + "' was not found", ex);
            }
        }

        EventType eventType = beanEventAdapter.createBeanType(eventTypeName, clazz, isPreconfiguredStatic, isPreconfigured, isConfigured);
        nameToTypeMap.put(eventTypeName, eventType);

        return eventType;
    }

    public synchronized EventType addNestableMapType(String eventTypeName, Map<String, Object> propertyTypesMayHavePrimitive, ConfigurationEventTypeMap optionalConfig, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured, boolean namedWindow, boolean insertInto) throws EventAdapterException {
        Pair<EventType[], Set<EventType>> mapSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(optionalConfig, EventUnderlyingType.MAP, nameToTypeMap);
        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.MAP, eventTypeName, isPreconfiguredStatic, isPreconfigured, isConfigured, namedWindow, insertInto);

        int typeId = eventTypeIdGenerator.getTypeId(eventTypeName);
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        MapEventType newEventType = new MapEventType(metadata, eventTypeName, typeId, this, propertyTypes, mapSuperTypes.getFirst(), mapSuperTypes.getSecond(), optionalConfig);

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            // The existing type must be the same as the type createdStatement
            if (!newEventType.equalsCompareType(existingType)) {
                String message = newEventType.getEqualsMessage(existingType);
                throw new EventAdapterException("Event type named '" + eventTypeName +
                        "' has already been declared with differing column name or type information: " + message);
            }

            // Since it's the same, return the existing type
            return existingType;
        }

        nameToTypeMap.put(eventTypeName, newEventType);

        return newEventType;
    }

    public synchronized EventType addNestableObjectArrayType(String eventTypeName, Map<String, Object> propertyTypesMayHavePrimitive, ConfigurationEventTypeObjectArray optionalConfig, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured, boolean namedWindow, boolean insertInto, boolean table, String tableName) throws EventAdapterException {
        if (optionalConfig != null && optionalConfig.getSuperTypes().size() > 1) {
            throw new EventAdapterException(ConfigurationEventTypeObjectArray.SINGLE_SUPERTYPE_MSG);
        }
        Pair<EventType[], Set<EventType>> mapSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(optionalConfig, EventUnderlyingType.OBJECTARRAY, nameToTypeMap);
        EventTypeMetadata metadata;
        if (table) {
            metadata = EventTypeMetadata.createTable(tableName);
        } else {
            metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.OBJECTARR, eventTypeName, isPreconfiguredStatic, isPreconfigured, isConfigured, namedWindow, insertInto);
        }

        int typeId = eventTypeIdGenerator.getTypeId(eventTypeName);
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        ObjectArrayEventType newEventType = new ObjectArrayEventType(metadata, eventTypeName, typeId, this, propertyTypes, optionalConfig, mapSuperTypes.getFirst(), mapSuperTypes.getSecond());

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            // The existing type must be the same as the type createdStatement
            if (!newEventType.equalsCompareType(existingType)) {
                String message = newEventType.getEqualsMessage(existingType);
                throw new EventAdapterException("Event type named '" + eventTypeName +
                        "' has already been declared with differing column name or type information: " + message);
            }

            // Since it's the same, return the existing type
            return existingType;
        }

        nameToTypeMap.put(eventTypeName, newEventType);

        return newEventType;
    }

    public EventBean adapterForMap(Map<String, Object> theEvent, String eventTypeName) throws EPException {
        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (!(existingType instanceof MapEventType)) {
            throw new EPException(EventAdapterServiceHelper.getMessageExpecting(eventTypeName, existingType, "Map"));
        }

        return adapterForTypedMap(theEvent, existingType);
    }

    public EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName) throws EPException {
        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (!(existingType instanceof ObjectArrayEventType)) {
            throw new EPException(EventAdapterServiceHelper.getMessageExpecting(eventTypeName, existingType, "Object-array"));
        }

        return adapterForTypedObjectArray(theEvent, existingType);
    }

    public EventBean adapterForDOM(Node node) {
        Node namedNode;
        if (node instanceof Document) {
            namedNode = ((Document) node).getDocumentElement();
        } else if (node instanceof Element) {
            namedNode = node;
        } else {
            throw new EPException("Unexpected DOM node of type '" + node.getClass() + "' encountered, please supply a Document or Element node");
        }

        String rootElementName = namedNode.getLocalName();
        if (rootElementName == null) {
            rootElementName = namedNode.getNodeName();
        }

        EventType eventType = xmldomRootElementNames.get(rootElementName);
        if (eventType == null) {
            throw new EventAdapterException("DOM event root element name '" + rootElementName +
                    "' has not been configured");
        }

        return new XMLEventBean(namedNode, eventType);
    }

    public EventBean adapterForTypedDOM(Node node, EventType eventType) {
        return new XMLEventBean(node, eventType);
    }

    /**
     * Add a configured XML DOM event type.
     *
     * @param eventTypeName                is the name name of the event type
     * @param configurationEventTypeXMLDOM configures the event type schema and namespace and XPath
     *                                     property information.
     */
    public synchronized EventType addXMLDOMType(String eventTypeName, ConfigurationEventTypeXMLDOM configurationEventTypeXMLDOM, SchemaModel optionalSchemaModel, boolean isPreconfiguredStatic) {
        return addXMLDOMType(eventTypeName, configurationEventTypeXMLDOM, optionalSchemaModel, isPreconfiguredStatic, false);
    }

    @Override
    public EventType replaceXMLEventType(String xmlEventTypeName, ConfigurationEventTypeXMLDOM config, SchemaModel schemaModel) {
        return addXMLDOMType(xmlEventTypeName, config, schemaModel, false, true);
    }

    /**
     * Add a configured XML DOM event type.
     *
     * @param eventTypeName                is the name name of the event type
     * @param configurationEventTypeXMLDOM configures the event type schema and namespace and XPath
     *                                     property information.
     */
    private synchronized EventType addXMLDOMType(String eventTypeName, ConfigurationEventTypeXMLDOM configurationEventTypeXMLDOM, SchemaModel optionalSchemaModel, boolean isPreconfiguredStatic, boolean allowOverrideExisting) {
        if (configurationEventTypeXMLDOM.getRootElementName() == null) {
            throw new EventAdapterException("Required root element name has not been supplied");
        }

        if (!allowOverrideExisting) {
            EventType existingType = nameToTypeMap.get(eventTypeName);
            if (existingType != null) {
                String message = "Event type named '" + eventTypeName + "' has already been declared with differing column name or type information";
                if (!(existingType instanceof BaseXMLEventType)) {
                    throw new EventAdapterException(message);
                }
                ConfigurationEventTypeXMLDOM config = ((BaseXMLEventType) existingType).getConfigurationEventTypeXMLDOM();
                if (!config.equals(configurationEventTypeXMLDOM)) {
                    throw new EventAdapterException(message);
                }

                return existingType;
            }
        }

        EventTypeMetadata metadata = EventTypeMetadata.createXMLType(eventTypeName, isPreconfiguredStatic, configurationEventTypeXMLDOM.getSchemaResource() == null && configurationEventTypeXMLDOM.getSchemaText() == null);
        EventType type;
        if ((configurationEventTypeXMLDOM.getSchemaResource() == null) && (configurationEventTypeXMLDOM.getSchemaText() == null)) {
            type = new SimpleXMLEventType(metadata, eventTypeIdGenerator.getTypeId(eventTypeName), configurationEventTypeXMLDOM, this);
        } else {
            if (optionalSchemaModel == null) {
                throw new EPException("Schema model has not been provided");
            }
            type = new SchemaXMLEventType(metadata, eventTypeIdGenerator.getTypeId(eventTypeName), configurationEventTypeXMLDOM, optionalSchemaModel, this);
        }

        nameToTypeMap.put(eventTypeName, type);
        xmldomRootElementNames.put(configurationEventTypeXMLDOM.getRootElementName(), type);

        return type;
    }

    public final EventBean adapterForType(Object theEvent, EventType eventType) {
        return EventAdapterServiceHelper.adapterForType(theEvent, eventType, this);
    }

    public final EventBean adapterForTypedMap(Map<String, Object> properties, EventType eventType) {
        return new MapEventBean(properties, eventType);
    }

    public final EventBean adapterForTypedObjectArray(Object[] properties, EventType eventType) {
        return new ObjectArrayEventBean(properties, eventType);
    }

    public synchronized EventType addWrapperType(String eventTypeName, EventType underlyingEventType, Map<String, Object> propertyTypesMayPrimitive, boolean isNamedWindow, boolean isInsertInto) throws EventAdapterException {
        // If we are wrapping an underlying type that is itself a wrapper, then this is a special case
        if (underlyingEventType instanceof WrapperEventType) {
            WrapperEventType underlyingWrapperType = (WrapperEventType) underlyingEventType;

            // the underlying type becomes the type already wrapped
            // properties are a superset of the wrapped properties and the additional properties
            underlyingEventType = underlyingWrapperType.getUnderlyingEventType();
            Map<String, Object> propertiesSuperset = new HashMap<String, Object>();
            propertiesSuperset.putAll(underlyingWrapperType.getUnderlyingMapType().getTypes());
            propertiesSuperset.putAll(propertyTypesMayPrimitive);
            propertyTypesMayPrimitive = propertiesSuperset;
        }

        boolean isPropertyAgnostic = false;
        if (underlyingEventType instanceof EventTypeSPI) {
            isPropertyAgnostic = ((EventTypeSPI) underlyingEventType).getMetadata().isPropertyAgnostic();
        }

        EventTypeMetadata metadata = EventTypeMetadata.createWrapper(eventTypeName, isNamedWindow, isInsertInto, isPropertyAgnostic);
        int typeId = eventTypeIdGenerator.getTypeId(eventTypeName);
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayPrimitive);
        WrapperEventType newEventType = new WrapperEventType(metadata, eventTypeName, typeId, underlyingEventType, propertyTypes, this);

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            // The existing type must be the same as the type created
            if (!newEventType.equalsCompareType(existingType)) {
                // It is possible that the wrapped event type is compatible: a child type of the desired type
                String message = isCompatibleWrapper(existingType, underlyingEventType, propertyTypes);
                if (message == null) {
                    return existingType;
                }

                throw new EventAdapterException("Event type named '" + eventTypeName +
                        "' has already been declared with differing column name or type information: " + message);
            }

            // Since it's the same, return the existing type
            return existingType;
        }

        nameToTypeMap.put(eventTypeName, newEventType);

        return newEventType;
    }

    /**
     * Returns true if the wrapper type is compatible with an existing wrapper type, for the reason that
     * the underlying event is a subtype of the existing underlying wrapper's type.
     *
     * @param existingType   is the existing wrapper type
     * @param underlyingType is the proposed new wrapper type's underlying type
     * @param propertyTypes  is the additional properties
     * @return true for compatible, or false if not
     */
    public static String isCompatibleWrapper(EventType existingType, EventType underlyingType, Map<String, Object> propertyTypes) {
        if (!(existingType instanceof WrapperEventType)) {
            return "Type '" + existingType.getName() + "' is not compatible";
        }
        WrapperEventType existingWrapper = (WrapperEventType) existingType;

        String message = MapEventType.isDeepEqualsProperties(existingType.getName(), existingWrapper.getUnderlyingMapType().getTypes(), propertyTypes);
        if (message != null) {
            return message;
        }
        EventType existingUnderlyingType = existingWrapper.getUnderlyingEventType();

        // If one of the supertypes of the underlying type is the existing underlying type, we are compatible
        if (underlyingType.getSuperTypes() == null) {
            return "Type '" + existingType.getName() + "' is not compatible";
        }
        for (Iterator<EventType> it = underlyingType.getDeepSuperTypes(); it.hasNext(); ) {
            EventType superUnderlying = it.next();
            if (superUnderlying == existingUnderlyingType) {
                return null;
            }
        }
        return "Type '" + existingType.getName() + "' is not compatible";
    }

    public final EventType createAnonymousMapType(String typeName, Map<String, Object> propertyTypesMayHavePrimitive, boolean isTransient) throws EventAdapterException {
        String assignedTypeName = EventAdapterService.ANONYMOUS_TYPE_NAME_PREFIX + typeName;
        EventTypeMetadata metadata = EventTypeMetadata.createAnonymous(assignedTypeName, EventTypeMetadata.ApplicationType.MAP);
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        MapEventType mapEventType = new MapEventType(metadata, assignedTypeName, eventTypeIdGenerator.getTypeId(assignedTypeName), this, propertyTypes, null, null, null);
        return anonymousTypeCache.addReturnExistingAnonymousType(mapEventType);
    }

    public final EventType createAnonymousObjectArrayType(String typeName, Map<String, Object> propertyTypesMayHavePrimitive) throws EventAdapterException {
        String assignedTypeName = EventAdapterService.ANONYMOUS_TYPE_NAME_PREFIX + typeName;
        EventTypeMetadata metadata = EventTypeMetadata.createAnonymous(assignedTypeName, EventTypeMetadata.ApplicationType.OBJECTARR);
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        ObjectArrayEventType oaEventType = new ObjectArrayEventType(metadata, assignedTypeName, eventTypeIdGenerator.getTypeId(assignedTypeName), this, propertyTypes, null, null, null);
        return anonymousTypeCache.addReturnExistingAnonymousType(oaEventType);
    }

    public final EventType createAnonymousAvroType(String typeName, Map<String, Object> properties, Annotation[] annotations, String statementName, String engineURI) throws EventAdapterException {
        String assignedTypeName = EventAdapterService.ANONYMOUS_TYPE_NAME_PREFIX + typeName;
        EventTypeMetadata metadata = EventTypeMetadata.createAnonymous(assignedTypeName, EventTypeMetadata.ApplicationType.AVRO);
        int typeId = eventTypeIdGenerator.getTypeId(assignedTypeName);
        EventType newEventType = avroHandler.newEventTypeFromNormalized(metadata, assignedTypeName, typeId, this, properties, annotations, null, null, null, statementName, engineURI);
        return anonymousTypeCache.addReturnExistingAnonymousType(newEventType);
    }

    public EventType createSemiAnonymousMapType(String typeName, Map<String, Pair<EventType, String>> taggedEventTypes, Map<String, Pair<EventType, String>> arrayEventTypes, boolean isUsedByChildViews) {
        Map<String, Object> mapProperties = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Pair<EventType, String>> entry : taggedEventTypes.entrySet()) {
            mapProperties.put(entry.getKey(), entry.getValue().getFirst());
        }
        for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
            mapProperties.put(entry.getKey(), new EventType[]{entry.getValue().getFirst()});
        }
        return createAnonymousMapType(typeName, mapProperties, true);
    }

    public final EventType createAnonymousWrapperType(String typeName, EventType underlyingEventType, Map<String, Object> propertyTypesMayPrimitive) throws EventAdapterException {
        String assignedTypeName = EventAdapterService.ANONYMOUS_TYPE_NAME_PREFIX + typeName;
        EventTypeMetadata metadata = EventTypeMetadata.createAnonymous(assignedTypeName, EventTypeMetadata.ApplicationType.WRAPPER);

        // If we are wrapping an underlying type that is itself a wrapper, then this is a special case: unwrap
        if (underlyingEventType instanceof WrapperEventType) {
            WrapperEventType underlyingWrapperType = (WrapperEventType) underlyingEventType;

            // the underlying type becomes the type already wrapped
            // properties are a superset of the wrapped properties and the additional properties
            underlyingEventType = underlyingWrapperType.getUnderlyingEventType();
            Map<String, Object> propertiesSuperset = new HashMap<String, Object>();
            propertiesSuperset.putAll(underlyingWrapperType.getUnderlyingMapType().getTypes());
            propertiesSuperset.putAll(propertyTypesMayPrimitive);
            propertyTypesMayPrimitive = propertiesSuperset;
        }

        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayPrimitive);
        WrapperEventType wrapperEventType = new WrapperEventType(metadata, assignedTypeName, eventTypeIdGenerator.getTypeId(assignedTypeName), underlyingEventType, propertyTypes, this);
        return anonymousTypeCache.addReturnExistingAnonymousType(wrapperEventType);
    }

    public final EventBean adapterForTypedWrapper(EventBean theEvent, Map<String, Object> properties, EventType eventType) {
        if (theEvent instanceof DecoratingEventBean) {
            DecoratingEventBean wrapper = (DecoratingEventBean) theEvent;
            properties.putAll(wrapper.getDecoratingProperties());
            return new WrapperEventBean(wrapper.getUnderlyingEvent(), properties, eventType);
        } else {
            return new WrapperEventBean(theEvent, properties, eventType);
        }
    }

    public final EventBean adapterForTypedBean(Object bean, EventType eventType) {
        return new BeanEventBean(bean, eventType);
    }

    public void addAutoNamePackage(String javaPackageName) {
        javaPackageNames.add(javaPackageName);
    }

    public EventType createAnonymousBeanType(String eventTypeName, Class clazz) {
        BeanEventType beanEventType = new BeanEventType(EventTypeMetadata.createBeanType(eventTypeName, clazz, false, false, false, EventTypeMetadata.TypeClass.ANONYMOUS),
                -1, clazz, this, beanEventAdapter.getClassToLegacyConfigs(clazz.getName()));
        return anonymousTypeCache.addReturnExistingAnonymousType(beanEventType);
    }

    public EventBean[] typeCast(List<EventBean> events, EventType targetType) {
        return EventAdapterServiceHelper.typeCast(events, targetType, this);
    }

    public boolean removeType(String name) {
        EventType eventType = nameToTypeMap.remove(name);
        if (eventType == null) {
            return false;
        }

        if (eventType instanceof BaseXMLEventType) {
            BaseXMLEventType baseXML = (BaseXMLEventType) eventType;
            xmldomRootElementNames.remove(baseXML.getRootElementName());
        }

        nameToHandlerMap.remove(name);
        return true;
    }

    public EventBeanSPI getShellForType(EventType eventType) {
        return EventAdapterServiceHelper.getShellForType(eventType);
    }

    public EventBeanAdapterFactory getAdapterFactoryForType(EventType eventType) {
        return EventAdapterServiceHelper.getAdapterFactoryForType(eventType);
    }

    public EventType addAvroType(String eventTypeName, ConfigurationEventTypeAvro avro, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured, boolean isNamedWindow, boolean isInsertInto) throws EventAdapterException {
        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.AVRO, eventTypeName, isPreconfiguredStatic, isPreconfigured, isConfigured, isNamedWindow, isInsertInto);

        int typeId = eventTypeIdGenerator.getTypeId(eventTypeName);
        Pair<EventType[], Set<EventType>> avroSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(avro, EventUnderlyingType.AVRO, nameToTypeMap);
        AvroSchemaEventType newEventType = avroHandler.newEventTypeFromSchema(metadata, eventTypeName, typeId, this, avro, avroSuperTypes.getFirst(), avroSuperTypes.getSecond());

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            avroHandler.validateExistingType(existingType, newEventType);
            return existingType;
        }

        nameToTypeMap.put(eventTypeName, newEventType);

        return newEventType;
    }

    public EventType addAvroType(String eventTypeName, Map<String, Object> types, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured, boolean isNamedWindow, boolean isInsertInto, Annotation[] annotations, ConfigurationEventTypeAvro config, String statementName, String engineURI) throws EventAdapterException {
        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.AVRO, eventTypeName, isPreconfiguredStatic, isPreconfigured, isConfigured, isNamedWindow, isInsertInto);

        int typeId = eventTypeIdGenerator.getTypeId(eventTypeName);
        Pair<EventType[], Set<EventType>> avroSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(config, EventUnderlyingType.AVRO, nameToTypeMap);
        AvroSchemaEventType newEventType = avroHandler.newEventTypeFromNormalized(metadata, eventTypeName, typeId, this, types, annotations, config, avroSuperTypes.getFirst(), avroSuperTypes.getSecond(), statementName, engineURI);

        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (existingType != null) {
            avroHandler.validateExistingType(existingType, newEventType);
            return existingType;
        }

        nameToTypeMap.put(eventTypeName, newEventType);

        return newEventType;
    }

    public EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        EventType existingType = nameToTypeMap.get(eventTypeName);
        if (!(existingType instanceof AvroSchemaEventType)) {
            throw new EPException(EventAdapterServiceHelper.getMessageExpecting(eventTypeName, existingType, "Avro"));
        }
        return avroHandler.adapterForTypeAvro(avroGenericDataDotRecord, existingType);
    }

    public EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType) {
        return avroHandler.adapterForTypeAvro(avroGenericDataDotRecord, eventType);
    }

    public EventAdapterAvroHandler getEventAdapterAvroHandler() {
        return avroHandler;
    }

    public TypeWidenerCustomizer getTypeWidenerCustomizer(EventType resultEventType) {
        return resultEventType instanceof AvroSchemaEventType ? avroHandler.getTypeWidenerCustomizer(resultEventType) : null;
    }
}
