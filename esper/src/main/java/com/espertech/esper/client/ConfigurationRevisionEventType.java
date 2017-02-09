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
package com.espertech.esper.client;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration information for revision event types.
 * <p>
 * The configuration information consists of the names of the base event type and the delta event types,
 * as well as the names of properties that supply key values, and a strategy.
 * <p>
 * Events of the base event type arrive before delta events; Delta events arriving before the base event
 * for the same key value are not processed, as delta events as well as base events represent new versions.
 */
public class ConfigurationRevisionEventType implements Serializable {
    private Set<String> nameBaseEventTypes;
    private Set<String> nameDeltaEventTypes;
    private PropertyRevision propertyRevision;
    private String[] keyPropertyNames;
    private static final long serialVersionUID = 2132650580293017453L;

    /**
     * Ctor.
     */
    public ConfigurationRevisionEventType() {
        nameBaseEventTypes = new HashSet<String>();
        nameDeltaEventTypes = new HashSet<String>();
        propertyRevision = PropertyRevision.OVERLAY_DECLARED;
    }

    /**
     * Add a base event type by it's name.
     *
     * @param nameBaseEventType the name of the base event type to add
     */
    public void addNameBaseEventType(String nameBaseEventType) {
        nameBaseEventTypes.add(nameBaseEventType);
    }

    /**
     * Returns the set of event type names that are base event types.
     *
     * @return names of base event types
     */
    public Set<String> getNameBaseEventTypes() {
        return nameBaseEventTypes;
    }

    /**
     * Returns the set of names of delta event types.
     *
     * @return names of delta event types
     */
    public Set<String> getNameDeltaEventTypes() {
        return nameDeltaEventTypes;
    }

    /**
     * Add a delta event type by it's name.
     *
     * @param nameDeltaEventType the name of the delta event type to add
     */
    public void addNameDeltaEventType(String nameDeltaEventType) {
        nameDeltaEventTypes.add(nameDeltaEventType);
    }

    /**
     * Returns the enumeration value defining the strategy to use for overlay or merging
     * multiple versions of an event (instances of base and delta events).
     *
     * @return strategy enumerator
     */
    public PropertyRevision getPropertyRevision() {
        return propertyRevision;
    }

    /**
     * Sets the enumeration value defining the strategy to use for overlay or merging
     * multiple versions of an event (instances of base and delta events).
     *
     * @param propertyRevision strategy enumerator
     */
    public void setPropertyRevision(PropertyRevision propertyRevision) {
        this.propertyRevision = propertyRevision;
    }

    /**
     * Returns the key property names, which are the names of the properties that supply key values for relating
     * base and delta events.
     *
     * @return array of names of key properties
     */
    public String[] getKeyPropertyNames() {
        return keyPropertyNames;
    }

    /**
     * Sets the key property names, which are the names of the properties that supply key values for relating
     * base and delta events.
     *
     * @param keyPropertyNames array of names of key properties
     */
    public void setKeyPropertyNames(String[] keyPropertyNames) {
        this.keyPropertyNames = keyPropertyNames;
    }

    /**
     * Enumeration for specifying a strategy to use to merge or overlay properties.
     */
    public enum PropertyRevision {
        /**
         * A fast strategy for revising events that groups properties provided by base and delta events and overlays contributed properties to compute a revision.
         * <p>
         * For use when there is a limited number of combinations of properties that change on an event, and such combinations are known in advance.
         * <p>
         * The properties available on the output revision events are all properties of the base event type. Delta event types do not add any additional properties that are not present on the base event type.
         * <p>
         * Any null values or non-existing property on a delta (or base) event results in a null values for the same property on the output revision event.
         */
        OVERLAY_DECLARED,

        /**
         * A strategy for revising events by merging properties provided by base and delta events, considering null values and non-existing (dynamic) properties as well.
         * <p>
         * For use when there is a limited number of combinations of properties that change on an event, and such combinations are known in advance.
         * <p>
         * The properties available on the output revision events are all properties of the base event type plus all additional properties that any of the delta event types provide.
         * <p>
         * Any null values or non-existing property on a delta (or base) event results in a null values for the same property on the output revision event.
         */
        MERGE_DECLARED,

        /**
         * A strategy for revising events by merging properties provided by base and delta events, considering only non-null values.
         * <p>
         * For use when there is an unlimited number of combinations of properties that change on an event, or combinations are not known in advance.
         * <p>
         * The properties available on the output revision events are all properties of the base event type plus all additional properties that any of the delta event types provide.
         * <p>
         * Null values returned by delta (or base) event properties provide no value to output revision events, i.e. null values are not merged.
         */
        MERGE_NON_NULL,

        /**
         * A strategy for revising events by merging properties provided by base and delta events, considering only values supplied by event properties that exist.
         * <p>
         * For use when there is an unlimited number of combinations of properties that change on an event, or combinations are not known in advance.
         * <p>
         * The properties available on the output revision events are all properties of the base event type plus all additional properties that any of the delta event types provide.
         * <p>
         * All properties are treated as dynamic properties: If an event property does not exist on a delta event (or base) event the property provides no value to output revision events, i.e. non-existing property values are not merged.
         */
        MERGE_EXISTS
    }
}
