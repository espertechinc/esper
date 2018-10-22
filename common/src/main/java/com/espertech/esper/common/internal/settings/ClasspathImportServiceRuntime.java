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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonMethodRef;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class ClasspathImportServiceRuntime extends ClasspathImportServiceBase {

    private final TimeZone timeZone;
    private final Map<String, ConfigurationCommonMethodRef> methodInvocationRef;

    public ClasspathImportServiceRuntime(Map<String, Object> transientConfiguration, TimeAbacus timeAbacus, Set<String> eventTypeAutoNames, TimeZone timeZone, Map<String, ConfigurationCommonMethodRef> methodInvocationRef, List<String> imports, List<String> annotationImports) {
        super(transientConfiguration, timeAbacus, eventTypeAutoNames);
        this.timeZone = timeZone;
        this.methodInvocationRef = methodInvocationRef;

        try {
            for (String importName : imports) {
                addImport(importName);
            }
            for (String importName : annotationImports) {
                addAnnotationImport(importName);
            }
        } catch (ClasspathImportException ex) {
            throw new ConfigurationException("Failed to process imports: " + ex.getMessage(), ex);
        }
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public ConfigurationCommonMethodRef getConfigurationMethodRef(String configurationName) {
        return methodInvocationRef.get(configurationName);
    }
}
