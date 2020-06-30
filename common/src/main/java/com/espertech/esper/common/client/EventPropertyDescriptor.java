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
package com.espertech.esper.common.client;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

import static com.espertech.esper.common.internal.event.core.EventTypeUtility.getPropertyTypeAsClass;

/**
 * Descriptor for event property names, property types and access metadata.
 */
public class EventPropertyDescriptor {
    private String propertyName;
    private EPType propertyType;
    private boolean isRequiresIndex;
    private boolean isRequiresMapkey;
    private boolean isIndexed;
    private boolean isMapped;
    private boolean isFragment;

    /**
     * Ctor.
     *
     * @param propertyName   name of the property
     * @param propertyType   the property type
     * @param requiresIndex  true if the access to property value access requires an integer index value
     * @param requiresMapkey true if the access to property value access requires a string map key
     * @param indexed        true if the property is an indexed property, i.e. type is an array or the property value access requires an integer index value
     * @param mapped         true if the property is a mapped property, i.e. type is an Map or the property value access requires an string map key
     * @param fragment       true if the property value can be represented as an EventBean and property type can be represented as an EventType
     */
    public EventPropertyDescriptor(String propertyName, EPType propertyType, boolean requiresIndex, boolean requiresMapkey, boolean indexed, boolean mapped, boolean fragment) {
        if (propertyType == null) {
            throw new IllegalArgumentException("Null property type");
        }
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        isRequiresIndex = requiresIndex;
        isRequiresMapkey = requiresMapkey;
        isIndexed = indexed;
        isMapped = mapped;
        isFragment = fragment;
    }

    /**
     * Returns the property name.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the property underlying type.
     * <p>
     * Note that a null values is possible as null values can be selected.
     * Use {@link #getPropertyEPType()} for access to type parameters.
     *
     * @return underlying property type
     */
    public Class getPropertyType() {
        return getPropertyTypeAsClass(propertyType);
    }

    /**
     * Returns the property underlying type.
     * <p>
     * Note that a null values is possible as null values can be selected.
     *
     * @return underlying property type
     */
    public EPType getPropertyEPType() {
        return propertyType;
    }

    /**
     * Returns the component type, if applicable.
     * This is applicable only to arrays and collections, queues and iterators.
     * Returns null if not applicable.
     *
     * @return component type
     */
    public Class getPropertyComponentType() {
        EPTypeClass typeClass = getPropertyComponentEPType();
        return typeClass == null ? null : typeClass.getType();
    }

    /**
     * Returns the component type, if applicable.
     * This is applicable only to arrays and collections, queues and iterators.
     * Returns null if not applicable.
     *
     * @return component type
     */
    public EPTypeClass getPropertyComponentEPType() {
        if (!(propertyType instanceof EPTypeClass)) {
            return null;
        }
        EPTypeClass type = (EPTypeClass) propertyType;
        if (type.getType().isArray()) {
            return JavaClassHelper.getArrayComponentType(type);
        }
        if (propertyType instanceof EPTypeClassParameterized) {
            EPTypeClassParameterized parameterized = (EPTypeClassParameterized) propertyType;
            if (JavaClassHelper.isImplementsInterface(parameterized, Collection.class) ||
                JavaClassHelper.isImplementsInterface(parameterized, Queue.class) ||
                JavaClassHelper.isImplementsInterface(parameterized, Iterable.class)) {
                return parameterized.getParameters()[0];
            }
            if (JavaClassHelper.isImplementsInterface(parameterized, Map.class)) {
                return parameterized.getParameters()[1];
            }
        } else {
            if (JavaClassHelper.isImplementsInterface(type, Collection.class) ||
                JavaClassHelper.isImplementsInterface(type, Queue.class) ||
                JavaClassHelper.isImplementsInterface(type, Iterable.class) ||
                JavaClassHelper.isImplementsInterface(type, Map.class)) {
                return EPTypePremade.OBJECT.getEPType();
            }
        }
        return null;
    }

    /**
     * Returns true to indicate that the property is an indexed property and requires an
     * index to access elements of the indexed property. Returns false to indicate that the
     * property is not an indexed property or does not require an index for property value access.
     * <p>
     * For JavaBean-style events, a getter-method that takes a single integer parameter
     * is considered an indexed property that requires an index for access.
     * <p>
     * A getter-method that returns an array is considered an index property but does not
     * require an index for access.
     *
     * @return true to indicate that property value access requires an index value
     */
    public boolean isRequiresIndex() {
        return isRequiresIndex;
    }

    /**
     * Returns true to indicate that the property is a mapped property and requires a
     * map key to access elements of the mapped property. Returns false to indicate that the
     * property is not a mapped property or does not require a map key for property value access.
     * <p>
     * For JavaBean-style events, a getter-method that takes a single string parameter
     * is considered a mapped property that requires a map key for access.
     * <p>
     * A getter-method that returns a Map is considered a mapped property but does not
     * require a map key for access.
     *
     * @return true to indicate that property value access requires an index value
     */
    public boolean isRequiresMapkey() {
        return isRequiresMapkey;
    }

    /**
     * Returns true for indexed properties, returns false for all other property styles.
     * <p>
     * An indexed property is a property returning an array value or a getter-method taking a single integer parameter.
     *
     * @return indicator whether this property is an index property
     */
    public boolean isIndexed() {
        return isIndexed;
    }

    /**
     * Returns true for mapped properties, returns false for all other property styles.
     * <p>
     * A mapped property is a property returning a Map value or a getter-method taking a single string (key) parameter.
     *
     * @return indicator whether this property is a mapped property
     */
    public boolean isMapped() {
        return isMapped;
    }

    /**
     * Returns true to indicate that the property value can itself be represented as an {@link EventBean}
     * and that the property type can be represented as an {@link EventType}.
     *
     * @return indicator whether property is itself a complex data structure representable as a nested {@link EventType}
     */
    public boolean isFragment() {
        return isFragment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventPropertyDescriptor that = (EventPropertyDescriptor) o;

        if (isFragment != that.isFragment) return false;
        if (isIndexed != that.isIndexed) return false;
        if (isMapped != that.isMapped) return false;
        if (isRequiresIndex != that.isRequiresIndex) return false;
        if (isRequiresMapkey != that.isRequiresMapkey) return false;
        if (!propertyName.equals(that.propertyName)) return false;
        if (propertyType != null ? !propertyType.equals(that.propertyType) : that.propertyType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = propertyName.hashCode();
        result = 31 * result + (propertyType != null ? propertyType.hashCode() : 0);
        result = 31 * result + (isRequiresIndex ? 1 : 0);
        result = 31 * result + (isRequiresMapkey ? 1 : 0);
        result = 31 * result + (isIndexed ? 1 : 0);
        result = 31 * result + (isMapped ? 1 : 0);
        result = 31 * result + (isFragment ? 1 : 0);
        return result;
    }

    public String toString() {
        return "name " + propertyName +
            " propertyType " + propertyType.getTypeName() +
            " isRequiresIndex " + isRequiresIndex +
            " isRequiresMapkey " + isRequiresMapkey +
            " isIndexed " + isIndexed +
            " isMapped " + isMapped +
            " isFragment " + isFragment;
    }
}
