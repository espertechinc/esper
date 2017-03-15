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
package com.espertech.esper.client.hook;

/**
 * For Avro customized type mapping, use with {@link TypeRepresentationMapper}
 */
public class TypeRepresentationMapperContext {
    private final Class clazz;
    private final String propertyName;
    private final String statementName;
    private final String engineURI;

    /**
     * Ctor
     * @param clazz class
     * @param propertyName property name
     * @param statementName statement name
     * @param engineURI engine URI
     */
    public TypeRepresentationMapperContext(Class clazz, String propertyName, String statementName, String engineURI) {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.statementName = statementName;
        this.engineURI = engineURI;
    }

    /**
     * Returns the class.
     * @return class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the property name
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the statement name
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the engine URI
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }
}
