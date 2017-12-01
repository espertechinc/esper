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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypesException;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.util.LevenshteinDistance;
import com.espertech.esper.util.StringValue;

public class ExprIdentNodeUtil {
    public static Pair<PropertyResolutionDescriptor, String> getTypeFromStream(StreamTypeService streamTypeService, String propertyNameNestable, boolean explicitPropertiesOnly, boolean obtainFragment)
            throws ExprValidationPropertyException {
        String streamOrProp = null;
        String prop = propertyNameNestable;
        if (propertyNameNestable.indexOf('.') != -1) {
            prop = propertyNameNestable.substring(propertyNameNestable.indexOf('.') + 1);
            streamOrProp = propertyNameNestable.substring(0, propertyNameNestable.indexOf('.'));
        }
        if (explicitPropertiesOnly) {
            return getTypeFromStreamExplicitProperties(streamTypeService, prop, streamOrProp, obtainFragment);
        }
        return getTypeFromStream(streamTypeService, prop, streamOrProp, obtainFragment);
    }

    protected static Pair<PropertyResolutionDescriptor, String> getTypeFromStream(StreamTypeService streamTypeService, String unresolvedPropertyName, String streamOrPropertyNameMayEscaped, boolean obtainFragment)
            throws ExprValidationPropertyException {
        PropertyResolutionDescriptor propertyInfo = null;

        // no stream/property name supplied
        if (streamOrPropertyNameMayEscaped == null) {
            try {
                propertyInfo = streamTypeService.resolveByPropertyName(unresolvedPropertyName, obtainFragment);
            } catch (StreamTypesException ex) {
                throw getSuggestionException(ex);
            } catch (PropertyAccessException ex) {
                throw new ExprValidationPropertyException("Failed to find property '" + unresolvedPropertyName + "', the property name does not parse (are you sure?): " + ex.getMessage(), ex);
            }

            // resolves without a stream name, return descriptor and null stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, propertyInfo.getStreamName());
        }

        // try to resolve the property name and stream name as it is (ie. stream name as a stream name)
        StreamTypesException typeExceptionOne;
        String streamOrPropertyName = StringValue.unescapeBacktick(streamOrPropertyNameMayEscaped);
        try {
            propertyInfo = streamTypeService.resolveByStreamAndPropName(streamOrPropertyName, unresolvedPropertyName, obtainFragment);
            // resolves with a stream name, return descriptor and stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, streamOrPropertyName);
        } catch (StreamTypesException ex) {
            typeExceptionOne = ex;
        }

        // try to resolve the property name to a nested property 's0.p0'
        StreamTypesException typeExceptionTwo;
        String propertyNameCandidate = streamOrPropertyName + '.' + unresolvedPropertyName;
        try {
            propertyInfo = streamTypeService.resolveByPropertyName(propertyNameCandidate, obtainFragment);
            // resolves without a stream name, return null for stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, null);
        } catch (StreamTypesException ex) {
            typeExceptionTwo = ex;
        }

        // not resolved yet, perhaps the table name did not match an event type
        if (streamTypeService.hasTableTypes() && streamOrPropertyName != null) {
            for (int i = 0; i < streamTypeService.getEventTypes().length; i++) {
                EventType eventType = streamTypeService.getEventTypes()[i];
                String tableName = TableServiceUtil.getTableNameFromEventType(eventType);
                if (tableName != null && tableName.equals(streamOrPropertyName)) {
                    try {
                        propertyInfo = streamTypeService.resolveByStreamAndPropName(eventType.getName(), unresolvedPropertyName, obtainFragment);
                    } catch (Exception ex) {
                    }
                    if (propertyInfo != null) {
                        return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, streamOrPropertyName);
                    }
                }
            }
        }

        // see if the stream or property name (the prefix) can be resolved by itself, without suffix
        // the property available may be indexed or mapped
        try {
            PropertyResolutionDescriptor desc = streamTypeService.resolveByPropertyName(streamOrPropertyName, false);
            if (desc != null) {
                EventPropertyDescriptor d2 = desc.getStreamEventType().getPropertyDescriptor(streamOrPropertyName);
                if (d2 != null) {
                    String text = null;
                    if (d2.isIndexed()) {
                        text = "an indexed property and requires an index or enumeration method to access values";
                    }
                    if (d2.isMapped()) {
                        text = "a mapped property and requires keyed access";
                    }
                    if (text != null) {
                        throw new ExprValidationPropertyException("Failed to resolve property '" + propertyNameCandidate + "' (property '" + streamOrPropertyName + "' is " + text + ")");
                    }
                }
            }
        } catch (StreamTypesException e) {
            // need not be handled
        }

        throw getSuggestionExceptionSecondStep(propertyNameCandidate, typeExceptionOne, typeExceptionTwo);
    }

    protected static Pair<PropertyResolutionDescriptor, String> getTypeFromStreamExplicitProperties(StreamTypeService streamTypeService, String unresolvedPropertyName, String streamOrPropertyName, boolean obtainFragment)
            throws ExprValidationPropertyException {
        PropertyResolutionDescriptor propertyInfo;

        // no stream/property name supplied
        if (streamOrPropertyName == null) {
            try {
                propertyInfo = streamTypeService.resolveByPropertyNameExplicitProps(unresolvedPropertyName, obtainFragment);
            } catch (StreamTypesException ex) {
                throw getSuggestionException(ex);
            } catch (PropertyAccessException ex) {
                throw new ExprValidationPropertyException(ex.getMessage());
            }

            // resolves without a stream name, return descriptor and null stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, propertyInfo.getStreamName());
        }

        // try to resolve the property name and stream name as it is (ie. stream name as a stream name)
        StreamTypesException typeExceptionOne;
        try {
            propertyInfo = streamTypeService.resolveByStreamAndPropNameExplicitProps(streamOrPropertyName, unresolvedPropertyName, obtainFragment);
            // resolves with a stream name, return descriptor and stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, streamOrPropertyName);
        } catch (StreamTypesException ex) {
            typeExceptionOne = ex;
        }

        // try to resolve the property name to a nested property 's0.p0'
        StreamTypesException typeExceptionTwo;
        String propertyNameCandidate = streamOrPropertyName + '.' + unresolvedPropertyName;
        try {
            propertyInfo = streamTypeService.resolveByPropertyNameExplicitProps(propertyNameCandidate, obtainFragment);
            // resolves without a stream name, return null for stream name
            return new Pair<PropertyResolutionDescriptor, String>(propertyInfo, null);
        } catch (StreamTypesException ex) {
            typeExceptionTwo = ex;
        }

        throw getSuggestionExceptionSecondStep(propertyNameCandidate, typeExceptionOne, typeExceptionTwo);
    }

    private static ExprValidationPropertyException getSuggestionExceptionSecondStep(String propertyNameCandidate, StreamTypesException typeExceptionOne, StreamTypesException typeExceptionTwo) {
        String suggestionOne = getSuggestion(typeExceptionOne);
        String suggestionTwo = getSuggestion(typeExceptionTwo);
        if (suggestionOne != null) {
            return new ExprValidationPropertyException(typeExceptionOne.getMessage() + suggestionOne);
        }
        if (suggestionTwo != null) {
            return new ExprValidationPropertyException(typeExceptionTwo.getMessage() + suggestionTwo);
        }

        // fail to resolve
        return new ExprValidationPropertyException("Failed to resolve property '" + propertyNameCandidate + "' to a stream or nested property in a stream");
    }

    private static ExprValidationPropertyException getSuggestionException(StreamTypesException ex) {
        String suggestion = getSuggestion(ex);
        if (suggestion != null) {
            return new ExprValidationPropertyException(ex.getMessage() + suggestion);
        } else {
            return new ExprValidationPropertyException(ex.getMessage());
        }
    }

    private static String getSuggestion(StreamTypesException ex) {
        if (ex == null) {
            return null;
        }
        Pair<Integer, String> suggestion = ex.getOptionalSuggestion();
        if (suggestion == null) {
            return null;
        }
        if (suggestion.getFirst() > LevenshteinDistance.ACCEPTABLE_DISTANCE) {
            return null;
        }
        return " (did you mean '" + ex.getOptionalSuggestion().getSecond() + "'?)";
    }
}
