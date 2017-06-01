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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.ConfigurationVariantStream;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.view.StatementStopService;

import java.util.*;

/**
 * Service for handling revision event types.
 * <p>
 * Each named window instance gets a dedicated revision processor.
 */
public class ValueAddEventServiceImpl implements ValueAddEventService {
    /**
     * Map of revision event name and revision compiled specification.
     */
    protected final Map<String, RevisionSpec> specificationsByRevisionName;

    /**
     * Map of named window name and processor.
     */
    protected final Map<String, ValueAddEventProcessor> processorsByNamedWindow;

    /**
     * Map of revision event stream and variant stream processor.
     */
    protected final Map<String, ValueAddEventProcessor> variantProcessors;

    /**
     * Ctor.
     */
    public ValueAddEventServiceImpl() {
        this.specificationsByRevisionName = new HashMap<String, RevisionSpec>();
        this.processorsByNamedWindow = new HashMap<String, ValueAddEventProcessor>();
        variantProcessors = new HashMap<String, ValueAddEventProcessor>();
    }

    public EventType[] getValueAddedTypes() {
        List<EventType> types = new ArrayList<EventType>();
        for (Map.Entry<String, ValueAddEventProcessor> revisionNamedWindow : processorsByNamedWindow.entrySet()) {
            types.add(revisionNamedWindow.getValue().getValueAddEventType());
        }
        for (Map.Entry<String, ValueAddEventProcessor> variantProcessor : variantProcessors.entrySet()) {
            types.add(variantProcessor.getValue().getValueAddEventType());
        }

        return types.toArray(new EventType[types.size()]);
    }

    public void init(Map<String, ConfigurationRevisionEventType> configRevision, Map<String, ConfigurationVariantStream> configVariant, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator)
            throws ConfigurationException {
        for (Map.Entry<String, ConfigurationRevisionEventType> entry : configRevision.entrySet()) {
            addRevisionEventType(entry.getKey(), entry.getValue(), eventAdapterService);
        }
        for (Map.Entry<String, ConfigurationVariantStream> entry : configVariant.entrySet()) {
            addVariantStream(entry.getKey(), entry.getValue(), eventAdapterService, eventTypeIdGenerator);
        }
    }

    public void addRevisionEventType(String revisioneventTypeName, ConfigurationRevisionEventType config, EventAdapterService eventAdapterService)
            throws ConfigurationException {
        RevisionSpec specification = validateRevision(revisioneventTypeName, config, eventAdapterService);
        specificationsByRevisionName.put(revisioneventTypeName, specification);
    }

    public void addVariantStream(String variantStreamname, ConfigurationVariantStream variantStreamConfig, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) throws ConfigurationException {
        VariantSpec variantSpec = validateVariantStream(variantStreamname, variantStreamConfig, eventAdapterService);
        VAEVariantProcessor processor = new VAEVariantProcessor(eventAdapterService, variantSpec, eventTypeIdGenerator, variantStreamConfig);
        eventAdapterService.addTypeByName(variantStreamname, processor.getValueAddEventType());
        variantProcessors.put(variantStreamname, processor);
    }

    /**
     * Validate the variant stream definition.
     *
     * @param variantStreamname   the stream name
     * @param variantStreamConfig the configuration information
     * @param eventAdapterService the event adapters
     * @return specification for variant streams
     */
    public static VariantSpec validateVariantStream(String variantStreamname, ConfigurationVariantStream variantStreamConfig, EventAdapterService eventAdapterService) {
        if (variantStreamConfig.getTypeVariance() == ConfigurationVariantStream.TypeVariance.PREDEFINED) {
            if (variantStreamConfig.getVariantTypeNames().isEmpty()) {
                throw new ConfigurationException("Invalid variant stream configuration, no event type name has been added and default type variance requires at least one type, for name '" + variantStreamname + "'");
            }
        }

        Set<EventType> types = new LinkedHashSet<EventType>();
        for (String typeName : variantStreamConfig.getVariantTypeNames()) {
            EventType type = eventAdapterService.getExistsTypeByName(typeName);
            if (type == null) {
                throw new ConfigurationException("Event type by name '" + typeName + "' could not be found for use in variant stream configuration by name '" + variantStreamname + "'");
            }
            types.add(type);
        }

        EventType[] eventTypes = types.toArray(new EventType[types.size()]);
        return new VariantSpec(variantStreamname, eventTypes, variantStreamConfig.getTypeVariance());
    }

    public EventType createRevisionType(String namedWindowName, String name, StatementStopService statementStopService, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) {
        RevisionSpec spec = specificationsByRevisionName.get(name);
        ValueAddEventProcessor processor;
        if (spec.getPropertyRevision() == ConfigurationRevisionEventType.PropertyRevision.OVERLAY_DECLARED) {
            processor = new VAERevisionProcessorDeclared(name, spec, statementStopService, eventAdapterService, eventTypeIdGenerator);
        } else {
            processor = new VAERevisionProcessorMerge(name, spec, statementStopService, eventAdapterService, eventTypeIdGenerator);
        }

        processorsByNamedWindow.put(namedWindowName, processor);
        return processor.getValueAddEventType();
    }

    public ValueAddEventProcessor getValueAddProcessor(String name) {
        ValueAddEventProcessor proc = processorsByNamedWindow.get(name);
        if (proc != null) {
            return proc;
        }
        return variantProcessors.get(name);
    }

    public EventType getValueAddUnderlyingType(String name) {
        RevisionSpec spec = specificationsByRevisionName.get(name);
        if (spec == null) {
            return null;
        }
        return spec.getBaseEventType();
    }

    public boolean isRevisionTypeName(String revisionTypeName) {
        return specificationsByRevisionName.containsKey(revisionTypeName);
    }

    /**
     * Valiate the revision configuration.
     *
     * @param revisioneventTypeName name of revision types
     * @param config                configures revision type
     * @param eventAdapterService   event adapters
     * @return revision specification
     * @throws ConfigurationException if the configs are invalid
     */
    protected static RevisionSpec validateRevision(String revisioneventTypeName, ConfigurationRevisionEventType config, EventAdapterService eventAdapterService)
            throws ConfigurationException {
        if ((config.getNameBaseEventTypes() == null) || (config.getNameBaseEventTypes().size() == 0)) {
            throw new ConfigurationException("Required base event type name is not set in the configuration for revision event type '" + revisioneventTypeName + "'");
        }

        if (config.getNameBaseEventTypes().size() > 1) {
            throw new ConfigurationException("Only one base event type name may be added to revision event type '" + revisioneventTypeName + "', multiple base types are not yet supported");
        }

        // get base types
        String baseeventTypeName = config.getNameBaseEventTypes().iterator().next();
        EventType baseEventType = eventAdapterService.getExistsTypeByName(baseeventTypeName);
        if (baseEventType == null) {
            throw new ConfigurationException("Could not locate event type for name '" + baseeventTypeName + "' in the configuration for revision event type '" + revisioneventTypeName + "'");
        }

        // get name types
        EventType[] deltaTypes = new EventType[config.getNameDeltaEventTypes().size()];
        String[] deltaNames = new String[config.getNameDeltaEventTypes().size()];
        int count = 0;
        for (String deltaName : config.getNameDeltaEventTypes()) {
            EventType deltaEventType = eventAdapterService.getExistsTypeByName(deltaName);
            if (deltaEventType == null) {
                throw new ConfigurationException("Could not locate event type for name '" + deltaName + "' in the configuration for revision event type '" + revisioneventTypeName + "'");
            }
            deltaTypes[count] = deltaEventType;
            deltaNames[count] = deltaName;
            count++;
        }

        // the key properties must be set
        if ((config.getKeyPropertyNames() == null) || (config.getKeyPropertyNames().length == 0)) {
            throw new ConfigurationException("Required key properties are not set in the configuration for revision event type '" + revisioneventTypeName + "'");
        }

        // make sure the key properties exist the base type and all delta types
        checkKeysExist(baseEventType, baseeventTypeName, config.getKeyPropertyNames(), revisioneventTypeName);
        for (int i = 0; i < deltaTypes.length; i++) {
            checkKeysExist(deltaTypes[i], deltaNames[i], config.getKeyPropertyNames(), revisioneventTypeName);
        }

        // key property names shared between base and delta must have the same type
        String[] keyPropertyNames = PropertyUtility.copyAndSort(config.getKeyPropertyNames());
        for (String key : keyPropertyNames) {
            Class typeProperty = baseEventType.getPropertyType(key);
            for (EventType dtype : deltaTypes) {
                Class dtypeProperty = dtype.getPropertyType(key);
                if ((dtypeProperty != null) && (typeProperty != dtypeProperty)) {
                    throw new ConfigurationException("Key property named '" + key + "' does not have the same type for base and delta types of revision event type '" + revisioneventTypeName + "'");
                }
            }
        }

        // In the "declared" type the change set properties consist of only :
        //   (base event type properties) minus (key properties) minus (properties only on base event type)
        if (config.getPropertyRevision() == ConfigurationRevisionEventType.PropertyRevision.OVERLAY_DECLARED) {
            // determine non-key properties: those overridden by any delta, and those simply only present on the base event type
            String[] nonkeyPropertyNames = PropertyUtility.uniqueExclusiveSort(baseEventType.getPropertyNames(), keyPropertyNames);
            Set<String> baseEventOnlyProperties = new HashSet<String>();
            Set<String> changesetPropertyNames = new HashSet<String>();
            for (String nonKey : nonkeyPropertyNames) {
                boolean overriddenProperty = false;
                for (EventType type : deltaTypes) {
                    if (type.isProperty(nonKey)) {
                        changesetPropertyNames.add(nonKey);
                        overriddenProperty = true;
                        break;
                    }
                }
                if (!overriddenProperty) {
                    baseEventOnlyProperties.add(nonKey);
                }
            }

            String[] changesetProperties = changesetPropertyNames.toArray(new String[changesetPropertyNames.size()]);
            String[] baseEventOnlyPropertyNames = baseEventOnlyProperties.toArray(new String[baseEventOnlyProperties.size()]);

            // verify that all changeset properties match event type
            for (String changesetProperty : changesetProperties) {
                Class typeProperty = baseEventType.getPropertyType(changesetProperty);
                for (EventType dtype : deltaTypes) {
                    Class dtypeProperty = dtype.getPropertyType(changesetProperty);
                    if ((dtypeProperty != null) && (typeProperty != dtypeProperty)) {
                        throw new ConfigurationException("Property named '" + changesetProperty + "' does not have the same type for base and delta types of revision event type '" + revisioneventTypeName + "'");
                    }
                }
            }

            return new RevisionSpec(config.getPropertyRevision(), baseEventType, deltaTypes, deltaNames, keyPropertyNames, changesetProperties, baseEventOnlyPropertyNames, false, null);
        } else {
            // In the "exists" type the change set properties consist of all properties: base event properties plus delta types properties
            Set<String> allProperties = new HashSet<String>();
            allProperties.addAll(Arrays.asList(baseEventType.getPropertyNames()));
            for (EventType deltaType : deltaTypes) {
                allProperties.addAll(Arrays.asList(deltaType.getPropertyNames()));
            }

            String[] allPropertiesArr = allProperties.toArray(new String[allProperties.size()]);
            String[] changesetProperties = PropertyUtility.uniqueExclusiveSort(allPropertiesArr, keyPropertyNames);

            // All properties must have the same type, if a property exists for any given type
            boolean hasContributedByDelta = false;
            boolean[] contributedByDelta = new boolean[changesetProperties.length];
            count = 0;
            for (String property : changesetProperties) {
                Class basePropertyType = baseEventType.getPropertyType(property);
                Class typeTemp = null;
                if (basePropertyType != null) {
                    typeTemp = basePropertyType;
                } else {
                    hasContributedByDelta = true;
                    contributedByDelta[count] = true;
                }
                for (EventType dtype : deltaTypes) {
                    Class dtypeProperty = dtype.getPropertyType(property);
                    if (dtypeProperty != null) {
                        if ((typeTemp != null) && (dtypeProperty != typeTemp)) {
                            throw new ConfigurationException("Property named '" + property + "' does not have the same type for base and delta types of revision event type '" + revisioneventTypeName + "'");
                        }

                    }
                    typeTemp = dtypeProperty;
                }
                count++;
            }

            // Compile changeset
            return new RevisionSpec(config.getPropertyRevision(), baseEventType, deltaTypes, deltaNames, keyPropertyNames, changesetProperties, new String[0], hasContributedByDelta, contributedByDelta);
        }
    }

    private static void checkKeysExist(EventType baseEventType, String name, String[] keyProperties, String revisioneventTypeName) {
        String[] propertyNames = baseEventType.getPropertyNames();
        for (String keyProperty : keyProperties) {
            boolean exists = false;
            for (String propertyName : propertyNames) {
                if (propertyName.equals(keyProperty)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                throw new ConfigurationException("Key property '" + keyProperty + "' as defined in the configuration for revision event type '" + revisioneventTypeName + "' does not exists in event type '" + name + "'");
            }
        }
    }
}
