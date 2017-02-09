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
import java.util.LinkedList;
import java.util.List;

/**
 * Configuration information for legacy (non-JavaBean) event types.
 */
public class ConfigurationEventTypeLegacy implements Serializable {
    private AccessorStyle accessorStyle;
    private CodeGeneration codeGeneration;
    private List<LegacyMethodPropDesc> methodProperties;
    private List<LegacyFieldPropDesc> fieldProperties;
    private Configuration.PropertyResolutionStyle propertyResolutionStyle;
    private String factoryMethod;
    private String copyMethod;
    private String startTimestampPropertyName;
    private String endTimestampPropertyName;
    private static final long serialVersionUID = 3385356145345570222L;

    /**
     * Ctor.
     */
    public ConfigurationEventTypeLegacy() {
        accessorStyle = AccessorStyle.JAVABEAN;
        codeGeneration = CodeGeneration.ENABLED;
        methodProperties = new LinkedList<LegacyMethodPropDesc>();
        fieldProperties = new LinkedList<LegacyFieldPropDesc>();
        propertyResolutionStyle = Configuration.PropertyResolutionStyle.CASE_SENSITIVE;
    }

    /**
     * Accessor style defines the methods of a class that are automatically exposed via event property.
     */
    public enum AccessorStyle {     // ensure the names match the configuration schema type restriction defs
        /**
         * Expose JavaBean-style getter methods only, plus explicitly configured properties.
         */
        JAVABEAN,

        /**
         * Expose only the explicitly configured methods and public members as event properties.
         */
        EXPLICIT,

        /**
         * Expose all public methods and public members as event properties, plus explicitly configured properties.
         */
        PUBLIC
    }

    /**
     * Enum to control code generation.
     */
    public enum CodeGeneration {    // ensure the names match the configuration schema type restriction defs
        /**
         * Enables code generation.
         */
        ENABLED,

        /**
         * Dispables code generation.
         */
        DISABLED
    }

    /**
     * Sets the accessor style. Thus controls how the engine exposes event properties
     * based on a Java class's public methods and public member variables.
     *
     * @param accessorStyle is the style enum
     */
    public void setAccessorStyle(AccessorStyle accessorStyle) {
        this.accessorStyle = accessorStyle;
    }

    /**
     * Sets the code generation flags. Thus controls whether or not the engine
     * generates code for access to event property values.
     *
     * @param codeGeneration is the code generation enum
     */
    public void setCodeGeneration(CodeGeneration codeGeneration) {
        this.codeGeneration = codeGeneration;
    }

    /**
     * Returns the accessor style.
     *
     * @return accessor style
     */
    public AccessorStyle getAccessorStyle() {
        return accessorStyle;
    }

    /**
     * Returns the code generation flag.
     *
     * @return code generation flag
     */
    public CodeGeneration getCodeGeneration() {
        return codeGeneration;
    }

    /**
     * Returns a list of descriptors specifying explicitly configured method names
     * and their property name.
     *
     * @return list of explicit method-access descriptors
     */
    public List<LegacyMethodPropDesc> getMethodProperties() {
        return methodProperties;
    }

    /**
     * Returns a list of descriptors specifying explicitly configured field names
     * and their property name.
     *
     * @return list of explicit field-access descriptors
     */
    public List<LegacyFieldPropDesc> getFieldProperties() {
        return fieldProperties;
    }

    /**
     * Adds the named event property backed by the named accessor method.
     * <p>
     * The accessor method is expected to be a public method with no parameters
     * for simple event properties, or with a single integer parameter for indexed
     * event properties, or with a single String parameter for mapped event properties.
     *
     * @param name           is the event property name
     * @param accessorMethod is the accessor method name.
     */
    public void addMethodProperty(String name, String accessorMethod) {
        methodProperties.add(new LegacyMethodPropDesc(name, accessorMethod));
    }

    /**
     * Adds the named event property backed by the named accessor field.
     *
     * @param name          is the event property name
     * @param accessorField is the accessor field underlying the name
     */
    public void addFieldProperty(String name, String accessorField) {
        fieldProperties.add(new LegacyFieldPropDesc(name, accessorField));
    }

    /**
     * Returns the type's property resolution style to use.
     *
     * @return property resolution style
     */
    public Configuration.PropertyResolutionStyle getPropertyResolutionStyle() {
        return propertyResolutionStyle;
    }

    /**
     * Sets the type's property resolution style to use.
     *
     * @param propertyResolutionStyle is the property resolution style to use for the type
     */
    public void setPropertyResolutionStyle(Configuration.PropertyResolutionStyle propertyResolutionStyle) {
        this.propertyResolutionStyle = propertyResolutionStyle;
    }

    /**
     * Returns the name of the factory method, either fully-qualified or just a method name if the
     * method is on the same class as the configured class, to use when instantiating
     * objects of the type.
     *
     * @return factory methods
     */
    public String getFactoryMethod() {
        return factoryMethod;
    }

    /**
     * Returns the name of the factory method, either fully-qualified or just a method name if the
     * method is on the same class as the configured class, to use when instantiating
     * objects of the type.
     *
     * @param factoryMethod factory methods
     */
    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    /**
     * Returns the method name of the method to use to copy the underlying event object.
     *
     * @return method name
     */
    public String getCopyMethod() {
        return copyMethod;
    }

    /**
     * Sets the method name of the method to use to copy the underlying event object.
     *
     * @param copyMethod method name
     */
    public void setCopyMethod(String copyMethod) {
        this.copyMethod = copyMethod;
    }

    /**
     * Returns the property name of the property providing the start timestamp value.
     *
     * @return start timestamp property name
     */
    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    /**
     * Sets the property name of the property providing the start timestamp value.
     *
     * @param startTimestampPropertyName start timestamp property name
     */
    public void setStartTimestampPropertyName(String startTimestampPropertyName) {
        this.startTimestampPropertyName = startTimestampPropertyName;
    }

    /**
     * Returns the property name of the property providing the end timestamp value.
     *
     * @return end timestamp property name
     */
    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    /**
     * Sets the property name of the property providing the end timestamp value.
     *
     * @param endTimestampPropertyName start timestamp property name
     */
    public void setEndTimestampPropertyName(String endTimestampPropertyName) {
        this.endTimestampPropertyName = endTimestampPropertyName;
    }

    /**
     * Encapsulates information about an accessor field backing a named event property.
     */
    public static class LegacyFieldPropDesc implements Serializable {
        private String name;
        private String accessorFieldName;
        private static final long serialVersionUID = 3725953138684324339L;

        /**
         * Ctor.
         *
         * @param name              is the event property name
         * @param accessorFieldName is the accessor field name
         */
        public LegacyFieldPropDesc(String name, String accessorFieldName) {
            this.name = name;
            this.accessorFieldName = accessorFieldName;
        }

        /**
         * Returns the event property name.
         *
         * @return event property name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the accessor field name.
         *
         * @return accessor field name
         */
        public String getAccessorFieldName() {
            return accessorFieldName;
        }
    }

    /**
     * Encapsulates information about an accessor method backing a named event property.
     */
    public static class LegacyMethodPropDesc implements Serializable {
        private String name;
        private String accessorMethodName;
        private static final long serialVersionUID = 3510051879181321459L;

        /**
         * Ctor.
         *
         * @param name               is the event property name
         * @param accessorMethodName is the name of the accessor method
         */
        public LegacyMethodPropDesc(String name, String accessorMethodName) {
            this.name = name;
            this.accessorMethodName = accessorMethodName;
        }

        /**
         * Returns the event property name.
         *
         * @return event property name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the accessor method name.
         *
         * @return accessor method name
         */
        public String getAccessorMethodName() {
            return accessorMethodName;
        }
    }
}
