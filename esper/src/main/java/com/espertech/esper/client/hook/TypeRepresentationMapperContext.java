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

package com.espertech.esper.client.hook;

public class TypeRepresentationMapperContext {
    private final Class clazz;
    private final String propertyName;
    private final String statementName;
    private final String engineURI;

    public TypeRepresentationMapperContext(Class clazz, String propertyName, String statementName, String engineURI) {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.statementName = statementName;
        this.engineURI = engineURI;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getEngineURI() {
        return engineURI;
    }
}
