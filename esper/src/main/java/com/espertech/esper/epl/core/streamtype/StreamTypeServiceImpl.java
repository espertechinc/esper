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
package com.espertech.esper.epl.core.streamtype;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.util.EPServiceProviderName;
import com.espertech.esper.util.LevenshteinDistance;
import com.espertech.esper.util.StringValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation that provides stream number and property type information.
 */
public class StreamTypeServiceImpl implements StreamTypeService {
    private final EventType[] eventTypes;
    private final String[] streamNames;
    private final boolean[] isIStreamOnly;
    private final String engineURIQualifier;
    private boolean isStreamZeroUnambigous;
    private boolean requireStreamNames;
    private boolean isOnDemandStreams;
    private boolean hasTableTypes;
    private boolean optionalStreams;

    /**
     * Ctor.
     *
     * @param engineURI         engine URI
     * @param isOnDemandStreams for on-demand stream
     */
    public StreamTypeServiceImpl(String engineURI, boolean isOnDemandStreams) {
        this(new EventType[0], new String[0], new boolean[0], engineURI, isOnDemandStreams, false);
    }

    /**
     * Ctor.
     *
     * @param eventType     a single event type for a single stream
     * @param streamName    the stream name of the single stream
     * @param engineURI     engine URI
     * @param isIStreamOnly true for no datawindow for stream
     */
    public StreamTypeServiceImpl(EventType eventType, String streamName, boolean isIStreamOnly, String engineURI) {
        this(new EventType[]{eventType}, new String[]{streamName}, new boolean[]{isIStreamOnly}, engineURI, false, false);
    }

    /**
     * Ctor.
     *
     * @param eventTypes        - array of event types, one for each stream
     * @param streamNames       - array of stream names, one for each stream
     * @param isIStreamOnly     true for no datawindow for stream
     * @param engineURI         - engine URI
     * @param isOnDemandStreams - true to indicate that all streams are on-demand pull-based
     * @param optionalStreams   - if there are any streams that may not provide events, applicable to outer joins
     */
    public StreamTypeServiceImpl(EventType[] eventTypes, String[] streamNames, boolean[] isIStreamOnly, String engineURI, boolean isOnDemandStreams, boolean optionalStreams) {
        this.eventTypes = eventTypes;
        this.streamNames = streamNames;
        this.isIStreamOnly = isIStreamOnly;
        this.isOnDemandStreams = isOnDemandStreams;
        this.optionalStreams = optionalStreams;

        if (engineURI == null || EPServiceProviderName.DEFAULT_ENGINE_URI.equals(engineURI)) {
            engineURIQualifier = EPServiceProviderName.DEFAULT_ENGINE_URI_QUALIFIER;
        } else {
            engineURIQualifier = engineURI;
        }

        if (eventTypes.length != streamNames.length) {
            throw new IllegalArgumentException("Number of entries for event types and stream names differs");
        }
        hasTableTypes = determineHasTableTypes();
    }

    /**
     * Ctor.
     *
     * @param namesAndTypes          is the ordered list of stream names and event types available (stream zero to N)
     * @param isStreamZeroUnambigous indicates whether when a property is found in stream zero and another stream an exception should be
     *                               thrown or the stream zero should be assumed
     * @param engineURI              uri of the engine
     * @param requireStreamNames     is true to indicate that stream names are required for any non-zero streams (for subqueries)
     */
    public StreamTypeServiceImpl(LinkedHashMap<String, Pair<EventType, String>> namesAndTypes, String engineURI, boolean isStreamZeroUnambigous, boolean requireStreamNames) {
        this.isStreamZeroUnambigous = isStreamZeroUnambigous;
        this.requireStreamNames = requireStreamNames;
        this.engineURIQualifier = engineURI;
        this.isIStreamOnly = new boolean[namesAndTypes.size()];
        eventTypes = new EventType[namesAndTypes.size()];
        streamNames = new String[namesAndTypes.size()];
        int count = 0;
        for (Map.Entry<String, Pair<EventType, String>> entry : namesAndTypes.entrySet()) {
            streamNames[count] = entry.getKey();
            eventTypes[count] = entry.getValue().getFirst();
            count++;
        }
        hasTableTypes = determineHasTableTypes();
        optionalStreams = true;
    }

    private boolean determineHasTableTypes() {
        for (EventType type : eventTypes) {
            if (type instanceof EventTypeSPI) {
                EventTypeSPI typeSPI = (EventTypeSPI) type;
                if (typeSPI.getMetadata().getTypeClass() == EventTypeMetadata.TypeClass.TABLE) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOptionalStreams() {
        return optionalStreams;
    }

    public void setRequireStreamNames(boolean requireStreamNames) {
        this.requireStreamNames = requireStreamNames;
    }

    public boolean isOnDemandStreams() {
        return isOnDemandStreams;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public String[] getStreamNames() {
        return streamNames;
    }

    public boolean[] getIStreamOnly() {
        return isIStreamOnly;
    }

    public boolean getOptionalStreams() {
        return optionalStreams;
    }

    public int getStreamNumForStreamName(String streamWildcard) {
        for (int i = 0; i < streamNames.length; i++) {
            if (streamWildcard.equals(streamNames[i])) {
                return i;
            }
        }
        return -1;
    }

    public PropertyResolutionDescriptor resolveByPropertyName(String propertyName, boolean obtainFragment)
            throws DuplicatePropertyException, PropertyNotFoundException {
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        PropertyResolutionDescriptor desc = findByPropertyName(propertyName, obtainFragment);
        if (requireStreamNames && (desc.getStreamNum() != 0)) {
            throw new PropertyNotFoundException("Property named '" + propertyName + "' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\"", null);
        }
        return desc;
    }

    public PropertyResolutionDescriptor resolveByPropertyNameExplicitProps(String propertyName, boolean obtainFragment) throws PropertyNotFoundException, DuplicatePropertyException {
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        PropertyResolutionDescriptor desc = findByPropertyNameExplicitProps(propertyName, obtainFragment);
        if (requireStreamNames && (desc.getStreamNum() != 0)) {
            throw new PropertyNotFoundException("Property named '" + propertyName + "' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\"", null);
        }
        return desc;
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamName, String propertyName, boolean obtainFragment)
            throws PropertyNotFoundException, StreamNotFoundException {
        if (streamName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        return findByStreamAndEngineName(propertyName, streamName, false, obtainFragment);
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropNameExplicitProps(String streamName, String propertyName, boolean obtainFragment) throws PropertyNotFoundException, StreamNotFoundException {
        if (streamName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }
        return findByStreamAndEngineName(propertyName, streamName, true, obtainFragment);
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamAndPropertyName, boolean obtainFragment) throws DuplicatePropertyException, PropertyNotFoundException {
        if (streamAndPropertyName == null) {
            throw new IllegalArgumentException("Null stream and property name");
        }

        PropertyResolutionDescriptor desc;
        try {
            // first try to resolve as a property name
            desc = findByPropertyName(streamAndPropertyName, obtainFragment);
        } catch (PropertyNotFoundException ex) {
            // Attempt to resolve by extracting a stream name
            int index = StringValue.unescapedIndexOfDot(streamAndPropertyName);
            if (index == -1) {
                throw ex;
            }
            String streamName = streamAndPropertyName.substring(0, index);
            String propertyName = streamAndPropertyName.substring(index + 1, streamAndPropertyName.length());
            try {
                // try to resolve a stream and property name
                desc = findByStreamAndEngineName(propertyName, streamName, false, obtainFragment);
            } catch (StreamNotFoundException e) {
                // Consider the engine URI as a further prefix
                Pair<String, String> propertyNoEnginePair = getIsEngineQualified(propertyName, streamName);
                if (propertyNoEnginePair == null) {
                    throw ex;
                }
                try {
                    return findByStreamNameOnly(propertyNoEnginePair.getFirst(), propertyNoEnginePair.getSecond(), false, obtainFragment);
                } catch (StreamNotFoundException e1) {
                    throw ex;
                }
            }
            return desc;
        }

        return desc;
    }

    private PropertyResolutionDescriptor findByPropertyName(String propertyName, boolean obtainFragment)
            throws DuplicatePropertyException, PropertyNotFoundException {
        int index = 0;
        int foundIndex = 0;
        int foundCount = 0;
        EventType streamType = null;

        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] != null) {
                Class propertyType = null;
                boolean found = false;
                FragmentEventType fragmentEventType = null;

                if (eventTypes[i].isProperty(propertyName)) {
                    propertyType = eventTypes[i].getPropertyType(propertyName);
                    if (obtainFragment) {
                        fragmentEventType = eventTypes[i].getFragmentType(propertyName);
                    }
                    found = true;
                } else {
                    // mapped(expression) or array(expression) are not property names but expressions against a property by name "mapped" or "array"
                    EventPropertyDescriptor descriptor = eventTypes[i].getPropertyDescriptor(propertyName);
                    if (descriptor != null) {
                        found = true;
                        propertyType = descriptor.getPropertyType();
                        if (descriptor.isFragment() && obtainFragment) {
                            fragmentEventType = eventTypes[i].getFragmentType(propertyName);
                        }
                    }
                }

                if (found) {
                    streamType = eventTypes[i];
                    foundCount++;
                    foundIndex = index;

                    // If the property could be resolved from stream 0 then we don't need to look further
                    if ((i == 0) && isStreamZeroUnambigous) {
                        return new PropertyResolutionDescriptor(streamNames[0], eventTypes[0], propertyName, 0, propertyType, fragmentEventType);
                    }
                }
            }
            index++;
        }

        handleFindExceptions(propertyName, foundCount, streamType);

        FragmentEventType fragmentEventType = null;
        if (obtainFragment) {
            fragmentEventType = streamType.getFragmentType(propertyName);
        }

        return new PropertyResolutionDescriptor(streamNames[foundIndex], eventTypes[foundIndex], propertyName, foundIndex, streamType.getPropertyType(propertyName), fragmentEventType);
    }

    private PropertyResolutionDescriptor findByPropertyNameExplicitProps(String propertyName, boolean obtainFragment)
            throws DuplicatePropertyException, PropertyNotFoundException {
        int index = 0;
        int foundIndex = 0;
        int foundCount = 0;
        EventType streamType = null;

        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] != null) {
                EventPropertyDescriptor[] descriptors = eventTypes[i].getPropertyDescriptors();
                Class propertyType = null;
                boolean found = false;
                FragmentEventType fragmentEventType = null;

                for (EventPropertyDescriptor desc : descriptors) {
                    if (desc.getPropertyName().equals(propertyName)) {
                        propertyType = desc.getPropertyType();
                        found = true;
                        if (obtainFragment && desc.isFragment()) {
                            fragmentEventType = eventTypes[i].getFragmentType(propertyName);
                        }
                    }
                }

                if (found) {
                    streamType = eventTypes[i];
                    foundCount++;
                    foundIndex = index;

                    // If the property could be resolved from stream 0 then we don't need to look further
                    if ((i == 0) && isStreamZeroUnambigous) {
                        return new PropertyResolutionDescriptor(streamNames[0], eventTypes[0], propertyName, 0, propertyType, fragmentEventType);
                    }
                }
            }
            index++;
        }

        handleFindExceptions(propertyName, foundCount, streamType);

        FragmentEventType fragmentEventType = null;
        if (obtainFragment) {
            fragmentEventType = streamType.getFragmentType(propertyName);
        }

        return new PropertyResolutionDescriptor(streamNames[foundIndex], eventTypes[foundIndex], propertyName, foundIndex, streamType.getPropertyType(propertyName), fragmentEventType);
    }

    private void handleFindExceptions(String propertyName, int foundCount, EventType streamType) throws DuplicatePropertyException, PropertyNotFoundException {
        if (foundCount > 1) {
            throw new DuplicatePropertyException("Property named '" + propertyName + "' is ambiguous as is valid for more then one stream");
        }

        if (streamType == null) {
            String message = "Property named '" + propertyName + "' is not valid in any stream";
            PropertyNotFoundExceptionSuggestionGenMultiTyped msgGen = new PropertyNotFoundExceptionSuggestionGenMultiTyped(eventTypes, propertyName);
            throw new PropertyNotFoundException(message, msgGen);
        }
    }

    private PropertyResolutionDescriptor findByStreamAndEngineName(String propertyName, String streamName, boolean explicitPropertiesOnly, boolean obtainFragment)
            throws PropertyNotFoundException, StreamNotFoundException {
        PropertyResolutionDescriptor desc;
        try {
            desc = findByStreamNameOnly(propertyName, streamName, explicitPropertiesOnly, obtainFragment);
        } catch (PropertyNotFoundException ex) {
            Pair<String, String> propertyNoEnginePair = getIsEngineQualified(propertyName, streamName);
            if (propertyNoEnginePair == null) {
                throw ex;
            }
            return findByStreamNameOnly(propertyNoEnginePair.getFirst(), propertyNoEnginePair.getSecond(), explicitPropertiesOnly, obtainFragment);
        } catch (StreamNotFoundException ex) {
            Pair<String, String> propertyNoEnginePair = getIsEngineQualified(propertyName, streamName);
            if (propertyNoEnginePair == null) {
                throw ex;
            }
            return findByStreamNameOnly(propertyNoEnginePair.getFirst(), propertyNoEnginePair.getSecond(), explicitPropertiesOnly, obtainFragment);
        }
        return desc;
    }

    private Pair<String, String> getIsEngineQualified(String propertyName, String streamName) {

        // If still not found, test for the stream name to contain the engine URI
        if (!streamName.equals(engineURIQualifier)) {
            return null;
        }

        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            return null;
        }

        String streamNameNoEngine = propertyName.substring(0, index);
        String propertyNameNoEngine = propertyName.substring(index + 1, propertyName.length());
        return new Pair<String, String>(propertyNameNoEngine, streamNameNoEngine);
    }

    private PropertyResolutionDescriptor findByStreamNameOnly(String propertyName, String streamName, boolean explicitPropertiesOnly, boolean obtainFragment)
            throws PropertyNotFoundException, StreamNotFoundException {
        int index = 0;
        EventType streamType = null;

        // Stream name resultion examples:
        // A)  select A1.price from Event.price as A2  => mismatch stream name, cannot resolve
        // B)  select Event1.price from Event2.price   => mismatch event type name, cannot resolve
        // C)  select default.Event2.price from Event2.price   => possible prefix of engine name
        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] == null) {
                index++;
                continue;
            }
            if ((streamNames[i] != null) && (streamNames[i].equals(streamName))) {
                streamType = eventTypes[i];
                break;
            }

            // If the stream name is the event type name, that is also acceptable
            if ((eventTypes[i].getName() != null) && (eventTypes[i].getName().equals(streamName))) {
                streamType = eventTypes[i];
                break;
            }

            index++;
        }

        if (streamType == null) {
            String message = "Failed to find a stream named '" + streamName + "'";
            StreamNotFoundExceptionSuggestionGen msgGen = new StreamNotFoundExceptionSuggestionGen(eventTypes, streamNames, streamName);
            throw new StreamNotFoundException(message, msgGen);
        }

        Class propertyType = null;
        FragmentEventType fragmentEventType = null;

        if (!explicitPropertiesOnly) {
            propertyType = streamType.getPropertyType(propertyName);
            if (propertyType == null) {
                EventPropertyDescriptor desc = streamType.getPropertyDescriptor(propertyName);
                if (desc == null) {
                    throw handlePropertyNotFound(propertyName, streamName, streamType);
                }
                propertyType = desc.getPropertyType();
                if (obtainFragment && desc.isFragment()) {
                    fragmentEventType = streamType.getFragmentType(propertyName);
                }
            } else {
                if (obtainFragment) {
                    fragmentEventType = streamType.getFragmentType(propertyName);
                }
            }
        } else {
            EventPropertyDescriptor[] explicitProps = streamType.getPropertyDescriptors();
            boolean found = false;
            for (EventPropertyDescriptor prop : explicitProps) {
                if (prop.getPropertyName().equals(propertyName)) {
                    propertyType = prop.getPropertyType();
                    if (obtainFragment && prop.isFragment()) {
                        fragmentEventType = streamType.getFragmentType(propertyName);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw handlePropertyNotFound(propertyName, streamName, streamType);
            }
        }

        return new PropertyResolutionDescriptor(streamName, streamType, propertyName, index, propertyType, fragmentEventType);
    }

    private PropertyNotFoundException handlePropertyNotFound(String propertyName, String streamName, EventType streamType) {
        String message = "Property named '" + propertyName + "' is not valid in stream '" + streamName + "'";
        PropertyNotFoundExceptionSuggestionGenSingleTyped msgGen = new PropertyNotFoundExceptionSuggestionGenSingleTyped(streamType, propertyName);
        return new PropertyNotFoundException(message, msgGen);
    }

    public String getEngineURIQualifier() {
        return engineURIQualifier;
    }

    public boolean hasPropertyAgnosticType() {
        for (EventType type : eventTypes) {
            if (type instanceof EventTypeSPI) {
                EventTypeSPI spi = (EventTypeSPI) type;
                if (spi.getMetadata().isPropertyAgnostic()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setStreamZeroUnambigous(boolean streamZeroUnambigous) {
        isStreamZeroUnambigous = streamZeroUnambigous;
    }

    public boolean hasTableTypes() {
        return hasTableTypes;
    }

    public boolean isStreamZeroUnambigous() {
        return isStreamZeroUnambigous;
    }

    private static class PropertyNotFoundExceptionSuggestionGenMultiTyped implements StreamTypesExceptionSuggestionGen {
        private final EventType[] eventTypes;
        private final String propertyName;

        public PropertyNotFoundExceptionSuggestionGenMultiTyped(EventType[] eventTypes, String propertyName) {
            this.eventTypes = eventTypes;
            this.propertyName = propertyName;
        }

        public Pair<Integer, String> getSuggestion() {
            return StreamTypeServiceUtil.findLevMatch(eventTypes, propertyName);
        }
    }

    private static class PropertyNotFoundExceptionSuggestionGenSingleTyped implements StreamTypesExceptionSuggestionGen {
        private final EventType eventType;
        private final String propertyName;

        public PropertyNotFoundExceptionSuggestionGenSingleTyped(EventType eventType, String propertyName) {
            this.eventType = eventType;
            this.propertyName = propertyName;
        }

        public Pair<Integer, String> getSuggestion() {
            return StreamTypeServiceUtil.findLevMatch(propertyName, eventType);
        }
    }

    private static class StreamNotFoundExceptionSuggestionGen implements StreamTypesExceptionSuggestionGen {
        private final EventType[] eventTypes;
        private final String[] streamNames;
        private final String streamName;

        public StreamNotFoundExceptionSuggestionGen(EventType[] eventTypes, String[] streamNames, String streamName) {
            this.eventTypes = eventTypes;
            this.streamNames = streamNames;
            this.streamName = streamName;
        }

        public Pair<Integer, String> getSuggestion() {

            // find a near match, textually
            String bestMatch = null;
            int bestMatchDiff = Integer.MAX_VALUE;

            for (int i = 0; i < eventTypes.length; i++) {
                if (streamNames[i] != null) {
                    int diff = LevenshteinDistance.computeLevenshteinDistance(streamNames[i], streamName);
                    if (diff < bestMatchDiff) {
                        bestMatchDiff = diff;
                        bestMatch = streamNames[i];
                    }
                }

                if (eventTypes[i] == null) {
                    continue;
                }

                // If the stream name is the event type name, that is also acceptable
                if (eventTypes[i].getName() != null) {
                    int diff = LevenshteinDistance.computeLevenshteinDistance(eventTypes[i].getName(), streamName);
                    if (diff < bestMatchDiff) {
                        bestMatchDiff = diff;
                        bestMatch = eventTypes[i].getName();
                    }
                }
            }

            Pair<Integer, String> suggestion = null;
            if (bestMatchDiff < Integer.MAX_VALUE) {
                suggestion = new Pair<Integer, String>(bestMatchDiff, bestMatch);
            }
            return suggestion;
        }
    }
}
