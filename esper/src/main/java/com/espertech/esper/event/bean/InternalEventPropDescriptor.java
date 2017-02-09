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
package com.espertech.esper.event.bean;

import com.espertech.esper.event.EventPropertyType;
import com.espertech.esper.event.property.GenericPropertyDesc;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Encapsulates the event property information available after introspecting an event's class members
 * for getter methods.
 */
public class InternalEventPropDescriptor {
    private String propertyName;
    private Method readMethod;
    private Field accessorField;
    private EventPropertyType propertyType;

    /**
     * Ctor.
     *
     * @param propertyName - name of property, from getter method
     * @param readMethod   - read method to get value
     * @param propertyType - type of property
     */
    public InternalEventPropDescriptor(String propertyName, Method readMethod, EventPropertyType propertyType) {
        this.propertyName = propertyName;
        this.readMethod = readMethod;
        this.propertyType = propertyType;
    }

    /**
     * Ctor.
     *
     * @param propertyName  - name of property, from getter method
     * @param accessorField - field to get value from
     * @param propertyType  - type of property
     */
    public InternalEventPropDescriptor(String propertyName, Field accessorField, EventPropertyType propertyType) {
        this.propertyName = propertyName;
        this.accessorField = accessorField;
        this.propertyType = propertyType;
    }

    /**
     * Return the property name, for mapped and indexed properties this is just the property name
     * without parantheses or brackets.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns an enum indicating the type of property: simple, mapped, indexed.
     *
     * @return enum with property type info
     */
    public EventPropertyType getPropertyType() {
        return propertyType;
    }

    /**
     * Returns the read method. Can return null if the property is backed by a field..
     *
     * @return read method of null if field property
     */
    public Method getReadMethod() {
        return readMethod;
    }

    /**
     * Returns the accessor field. Can return null if the property is backed by a method.
     *
     * @return accessor field of null if method property
     */
    public Field getAccessorField() {
        return accessorField;
    }

    /**
     * Returns the type of the underlying method or field of the event property.
     *
     * @return return type
     */
    public Class getReturnType() {
        if (readMethod != null) {
            return readMethod.getReturnType();
        } else {
            return accessorField.getType();
        }
    }

    /**
     * Returns the type of the underlying method or field of the event property.
     *
     * @return return type
     */
    public GenericPropertyDesc getReturnTypeGeneric() {
        if (readMethod != null) {
            return new GenericPropertyDesc(readMethod.getReturnType(), JavaClassHelper.getGenericReturnType(readMethod, true));
        } else {
            return new GenericPropertyDesc(accessorField.getType(), JavaClassHelper.getGenericFieldType(accessorField, true));
        }
    }

    public String toString() {
        return "propertyName=" + propertyName +
                " readMethod=" + readMethod +
                " accessorField=" + accessorField +
                " propertyType=" + propertyType;
    }

    public boolean equals(Object other) {
        if (!(other instanceof InternalEventPropDescriptor)) {
            return false;
        }
        InternalEventPropDescriptor otherDesc = (InternalEventPropDescriptor) other;
        if (!otherDesc.propertyName.equals(propertyName)) {
            return false;
        }
        if (((otherDesc.readMethod == null) && (readMethod != null)) ||
                ((otherDesc.readMethod != null) && (readMethod == null))) {
            return false;
        }
        if ((otherDesc.readMethod != null) && (readMethod != null) &&
                (!otherDesc.readMethod.equals(readMethod))) {
            return false;
        }
        if (((otherDesc.accessorField == null) && (accessorField != null)) ||
                ((otherDesc.accessorField != null) && (accessorField == null))) {
            return false;
        }
        if ((otherDesc.accessorField != null) && (accessorField != null) &&
                (!otherDesc.accessorField.equals(accessorField))) {
            return false;
        }
        if (otherDesc.propertyType != propertyType) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
