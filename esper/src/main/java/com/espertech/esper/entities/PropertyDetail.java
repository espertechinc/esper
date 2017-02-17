/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.entities;

/**
 * Event property name and type information.
 */
public class PropertyDetail {
    private String propertyName;
    private String propertyType;
    private String componentType;
    private boolean writable;
    private boolean requiresIndex;
    private boolean requiresMapkey;
    private boolean indexed;
    private boolean mapped;
    private String fragmentTypeName;
    private String fragmentTypeUnd;
    private Boolean fragmentIsIndexed;
    private Boolean fragmentIsNative;
    private PropertyDetail[] fragmentProperties;
    private String serializerClass;

    public PropertyDetail(String propertyName, String propertyType, String componentType, boolean writable, boolean requiresIndex, boolean requiresMapkey, boolean indexed, boolean mapped, String fragmentTypeName, String fragmentTypeUnd, Boolean fragmentIsIndexed, Boolean fragmentIsNative, PropertyDetail[] fragmentProperties, String serializerClass) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.componentType = componentType;
        this.writable = writable;
        this.requiresIndex = requiresIndex;
        this.requiresMapkey = requiresMapkey;
        this.indexed = indexed;
        this.mapped = mapped;
        this.fragmentTypeName = fragmentTypeName;
        this.fragmentTypeUnd = fragmentTypeUnd;
        this.fragmentIsIndexed = fragmentIsIndexed;
        this.fragmentIsNative = fragmentIsNative;
        this.fragmentProperties = fragmentProperties;
        this.serializerClass = serializerClass;
    }

    public PropertyDetail() {
    }

    /**
     * Returns the property name.
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the property type as a string that contains the class name of the type.
     * @return property type class name
     */
    public String getPropertyType() {
        return propertyType;
    }

    /**
     * Returns the type of the indexed property in the array or collection, i.e. the array or collection component type.
     * @return component type class name, or not populated if not an index property
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * Returns true if the property is writable.
     * @return writable indicator
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * Returns true if the property is an index property that requires an index for value access
     * @return indicator if indexed property that requires an index value for access
     */
    public boolean isRequiresIndex() {
        return requiresIndex;
    }

    /**
     * Returns true if the property is a mapped property that requires a map key for value access
     * @return indicator if mapped property that requires a map key for access
     */
    public boolean isRequiresMapkey() {
        return requiresMapkey;
    }

    /**
     * Returns true for indexed properties.
     * @return indicator if indexed property
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Returns true for mapped properties.
     * @return indicator if mapped property
     */
    public boolean isMapped() {
        return mapped;
    }

    /**
     * Returns the event type name of the property if the property's type is an event type itself
     * @return event type name or null
     */
    public String getFragmentTypeName() {
        return fragmentTypeName;
    }

    /**
     * Returns true if the property carries events and is an indexed property
     * @return indicator whether property carries events and is an indexed property
     */
    public Boolean getFragmentIsIndexed() {
        return fragmentIsIndexed;
    }

    /**
     * Returns true if the property carries an event (or multiple) and the event type is a native (POJO)
     * @return indicator that fragement type is POJO
     */
    public Boolean getFragmentIsNative() {
        return fragmentIsNative;
    }

    public PropertyDetail[] getFragmentProperties() {
        return fragmentProperties;
    }

    public String getSerializerClass() {
        return serializerClass;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public void setRequiresIndex(boolean requiresIndex) {
        this.requiresIndex = requiresIndex;
    }

    public void setRequiresMapkey(boolean requiresMapkey) {
        this.requiresMapkey = requiresMapkey;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }

    public void setFragmentTypeName(String fragmentTypeName) {
        this.fragmentTypeName = fragmentTypeName;
    }

    public void setFragmentIsIndexed(Boolean fragmentIsIndexed) {
        this.fragmentIsIndexed = fragmentIsIndexed;
    }

    public void setFragmentIsNative(Boolean fragmentIsNative) {
        this.fragmentIsNative = fragmentIsNative;
    }

    public void setFragmentProperties(PropertyDetail[] fragmentProperties) {
        this.fragmentProperties = fragmentProperties;
    }

    public void setSerializerClass(String serializerClass) {
        this.serializerClass = serializerClass;
    }

    /**
     * Get the underlying type of the fragement type, if the property is a fragment
     * @return underlying type of fragment
     */
    public String getFragmentTypeUnd() {
        return fragmentTypeUnd;
    }

    public void setFragmentTypeUnd(String fragmentTypeUnd) {
        this.fragmentTypeUnd = fragmentTypeUnd;
    }
}
