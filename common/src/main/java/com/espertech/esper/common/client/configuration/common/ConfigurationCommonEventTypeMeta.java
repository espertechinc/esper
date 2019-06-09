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
package com.espertech.esper.common.client.configuration.common;

import com.espertech.esper.common.client.hook.type.ObjectValueTypeWidenerFactory;
import com.espertech.esper.common.client.hook.type.TypeRepresentationMapper;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;

import java.io.Serializable;

/**
 * Event representation metadata.
 */
public class ConfigurationCommonEventTypeMeta implements Serializable {

    private static final long serialVersionUID = 1430881486279542799L;
    private PropertyResolutionStyle classPropertyResolutionStyle;
    private AccessorStyle defaultAccessorStyle;
    private EventUnderlyingType defaultEventRepresentation;
    private AvroSettings avroSettings;

    /**
     * Ctor.
     */
    public ConfigurationCommonEventTypeMeta() {
        this.classPropertyResolutionStyle = PropertyResolutionStyle.getDefault();
        this.defaultAccessorStyle = AccessorStyle.JAVABEAN;
        this.defaultEventRepresentation = EventUnderlyingType.getDefault();
        this.avroSettings = new AvroSettings();
    }

    /**
     * Returns the default accessor style, JavaBean unless changed.
     *
     * @return style enum
     */
    public AccessorStyle getDefaultAccessorStyle() {
        return defaultAccessorStyle;
    }

    /**
     * Sets the default accessor style, which is JavaBean unless changed.
     *
     * @param defaultAccessorStyle style enum
     */
    public void setDefaultAccessorStyle(AccessorStyle defaultAccessorStyle) {
        this.defaultAccessorStyle = defaultAccessorStyle;
    }

    /**
     * Returns the property resolution style to use for resolving property names
     * of Java classes.
     *
     * @return style of property resolution
     */
    public PropertyResolutionStyle getClassPropertyResolutionStyle() {
        return classPropertyResolutionStyle;
    }

    /**
     * Sets the property resolution style to use for resolving property names
     * of Java classes.
     *
     * @param classPropertyResolutionStyle style of property resolution
     */
    public void setClassPropertyResolutionStyle(PropertyResolutionStyle classPropertyResolutionStyle) {
        this.classPropertyResolutionStyle = classPropertyResolutionStyle;
    }

    /**
     * Sets the default event representation.
     *
     * @param defaultEventRepresentation to set
     */
    public void setDefaultEventRepresentation(EventUnderlyingType defaultEventRepresentation) {
        this.defaultEventRepresentation = defaultEventRepresentation;
    }

    /**
     * Returns the default event representation.
     *
     * @return setting
     */
    public EventUnderlyingType getDefaultEventRepresentation() {
        return defaultEventRepresentation;
    }

    /**
     * Returns the Avro settings.
     *
     * @return avro settings
     */
    public AvroSettings getAvroSettings() {
        return avroSettings;
    }

    /**
     * Sets the Avro settings.
     *
     * @param avroSettings avro settings
     */
    public void setAvroSettings(AvroSettings avroSettings) {
        this.avroSettings = avroSettings;
    }

    /**
     * Avro settings.
     */
    public static class AvroSettings implements Serializable {
        private static final long serialVersionUID = 2977645210525767203L;
        private boolean enableAvro = true;
        private boolean enableNativeString = true;
        private boolean enableSchemaDefaultNonNull = true;
        private String typeRepresentationMapperClass;
        private String objectValueTypeWidenerFactoryClass;

        /**
         * Returns the indicator whether Avro support is enabled when available (true by default).
         *
         * @return indicator
         */
        public boolean isEnableAvro() {
            return enableAvro;
        }

        /**
         * Sets the indicator whether Avro support is enabled when available (true by default).
         *
         * @param enableAvro indicator to set
         */
        public void setEnableAvro(boolean enableAvro) {
            this.enableAvro = enableAvro;
        }

        /**
         * Returns indicator whether for String-type values to use the "avro.java.string=String" (true by default)
         *
         * @return indicator
         */
        public boolean isEnableNativeString() {
            return enableNativeString;
        }

        /**
         * Sets indicator whether for String-type values to use the "avro.java.string=String" (true by default)
         *
         * @param enableNativeString indicator
         */
        public void setEnableNativeString(boolean enableNativeString) {
            this.enableNativeString = enableNativeString;
        }

        /**
         * Returns indicator whether generated schemas should assume non-null values (true by default)
         *
         * @return indicator
         */
        public boolean isEnableSchemaDefaultNonNull() {
            return enableSchemaDefaultNonNull;
        }

        /**
         * Sets indicator whether generated schemas should assume non-null values (true by default)
         *
         * @param enableSchemaDefaultNonNull indicator
         */
        public void setEnableSchemaDefaultNonNull(boolean enableSchemaDefaultNonNull) {
            this.enableSchemaDefaultNonNull = enableSchemaDefaultNonNull;
        }

        /**
         * Sets class name of mapping provider that maps types to an Avro schema; a mapper should implement {@link TypeRepresentationMapper}
         * (null by default, using default mapping)
         *
         * @param typeRepresentationMapperClass class name
         */
        public void setTypeRepresentationMapperClass(String typeRepresentationMapperClass) {
            this.typeRepresentationMapperClass = typeRepresentationMapperClass;
        }

        /**
         * Returns class name of mapping provider that maps types to an Avro schema; a mapper should implement {@link TypeRepresentationMapper}
         * (null by default, using default mapping)
         *
         * @return class name
         */
        public String getTypeRepresentationMapperClass() {
            return typeRepresentationMapperClass;
        }

        /**
         * Returns the class name of widening provider that widens, coerces or transforms object values to an Avro field value or record; a widener should implement {@link ObjectValueTypeWidenerFactory}
         * (null by default, using default widening)
         *
         * @return class name
         */
        public String getObjectValueTypeWidenerFactoryClass() {
            return objectValueTypeWidenerFactoryClass;
        }

        /**
         * Sets the class name of widening provider that widens, coerces or transforms object values to an Avro field value or record; a widener should implement {@link ObjectValueTypeWidenerFactory}
         * (null by default, using default widening)
         *
         * @param objectValueTypeWidenerFactoryClass class name
         */
        public void setObjectValueTypeWidenerFactoryClass(String objectValueTypeWidenerFactoryClass) {
            this.objectValueTypeWidenerFactoryClass = objectValueTypeWidenerFactoryClass;
        }
    }
}
