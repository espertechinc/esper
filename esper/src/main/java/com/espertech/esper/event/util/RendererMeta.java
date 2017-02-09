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
package com.espertech.esper.event.util;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Renderer cache for event type metadata allows fast rendering of a given type of events.
 */
public class RendererMeta {
    private static final Logger log = LoggerFactory.getLogger(RendererMeta.class);

    private final GetterPair[] simpleProperties;
    private final GetterPair[] indexProperties;
    private final GetterPair[] mappedProperties;
    private final NestedGetterPair[] nestedProperties;

    /**
     * Ctor.
     *
     * @param eventType to render
     * @param stack     the stack of properties to avoid looping
     * @param options   rendering options
     */
    public RendererMeta(EventType eventType, Stack<EventTypePropertyPair> stack, RendererMetaOptions options) {
        ArrayList<GetterPair> gettersSimple = new ArrayList<GetterPair>();
        ArrayList<GetterPair> gettersIndexed = new ArrayList<GetterPair>();
        ArrayList<GetterPair> gettersMapped = new ArrayList<GetterPair>();
        ArrayList<NestedGetterPair> gettersNested = new ArrayList<NestedGetterPair>();

        EventPropertyDescriptor[] descriptors = eventType.getPropertyDescriptors();
        for (EventPropertyDescriptor desc : descriptors) {
            String propertyName = desc.getPropertyName();

            if ((!desc.isIndexed()) && (!desc.isMapped()) && (!desc.isFragment())) {
                EventPropertyGetter getter = eventType.getGetter(propertyName);
                if (getter == null) {
                    log.warn("No getter returned for event type '" + eventType.getName() + "' and property '" + propertyName + "'");
                    continue;
                }
                gettersSimple.add(new GetterPair(getter, propertyName, OutputValueRendererFactory.getOutputValueRenderer(desc.getPropertyType(), options)));
            }

            if (desc.isIndexed() && !desc.isRequiresIndex() && (!desc.isFragment())) {
                EventPropertyGetter getter = eventType.getGetter(propertyName);
                if (getter == null) {
                    log.warn("No getter returned for event type '" + eventType.getName() + "' and property '" + propertyName + "'");
                    continue;
                }
                gettersIndexed.add(new GetterPair(getter, propertyName, OutputValueRendererFactory.getOutputValueRenderer(desc.getPropertyType(), options)));
            }

            if (desc.isMapped() && !desc.isRequiresMapkey() && (!desc.isFragment())) {
                EventPropertyGetter getter = eventType.getGetter(propertyName);
                if (getter == null) {
                    log.warn("No getter returned for event type '" + eventType.getName() + "' and property '" + propertyName + "'");
                    continue;
                }
                gettersMapped.add(new GetterPair(getter, propertyName, OutputValueRendererFactory.getOutputValueRenderer(desc.getPropertyType(), options)));
            }

            if (desc.isFragment()) {
                EventPropertyGetter getter = eventType.getGetter(propertyName);
                FragmentEventType fragmentType = eventType.getFragmentType(propertyName);
                if (getter == null) {
                    log.warn("No getter returned for event type '" + eventType.getName() + "' and property '" + propertyName + "'");
                    continue;
                }
                if (fragmentType == null) {
                    log.warn("No fragment type returned for event type '" + eventType.getName() + "' and property '" + propertyName + "'");
                    continue;
                }

                EventTypePropertyPair pair = new EventTypePropertyPair(fragmentType.getFragmentType(), propertyName);
                if (options.isPreventLooping() && stack.contains(pair)) {
                    continue;   // prevent looping behavior on self-references
                }

                stack.push(pair);
                RendererMeta fragmentMetaData = new RendererMeta(fragmentType.getFragmentType(), stack, options);
                stack.pop();

                gettersNested.add(new NestedGetterPair(getter, propertyName, fragmentMetaData, fragmentType.isIndexed()));
            }
        }

        simpleProperties = gettersSimple.toArray(new GetterPair[gettersSimple.size()]);
        indexProperties = gettersIndexed.toArray(new GetterPair[gettersIndexed.size()]);
        mappedProperties = gettersMapped.toArray(new GetterPair[gettersMapped.size()]);
        nestedProperties = gettersNested.toArray(new NestedGetterPair[gettersNested.size()]);
    }

    /**
     * Returns simple properties.
     *
     * @return properties
     */
    public GetterPair[] getSimpleProperties() {
        return simpleProperties;
    }

    /**
     * Returns index properties.
     *
     * @return properties
     */
    public GetterPair[] getIndexProperties() {
        return indexProperties;
    }

    /**
     * Returns nested properties.
     *
     * @return properties
     */
    public NestedGetterPair[] getNestedProperties() {
        return nestedProperties;
    }

    /**
     * Returns mapped properties.
     *
     * @return mapped props
     */
    public GetterPair[] getMappedProperties() {
        return mappedProperties;
    }
}
