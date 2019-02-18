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
package com.espertech.esper.runtime.internal.kernel.faf;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPModuleEventTypeInitServicesImpl;
import com.espertech.esper.common.internal.context.query.FAFProvider;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodProvider;
import com.espertech.esper.common.internal.event.path.EventTypeCollectorImpl;
import com.espertech.esper.common.internal.event.path.EventTypeResolverImpl;
import com.espertech.esper.runtime.client.util.RuntimeVersion;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementInitServicesImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EPRuntimeHelperFAF {
    public static FAFProvider queryMethod(EPCompiled compiled, EPServicesContext services) {
        ByteArrayProvidingClassLoader classLoader = new ByteArrayProvidingClassLoader(compiled.getClasses(), services.getClasspathImportServiceRuntime().getClassLoader());

        try {
            RuntimeVersion.checkVersion(compiled.getManifest().getCompilerVersion());
        } catch (RuntimeVersion.VersionException ex) {
            throw new EPException(ex.getMessage(), ex);
        }

        if (compiled.getManifest().getQueryProviderClassName() == null) {
            if (compiled.getManifest().getModuleProviderClassName() != null) {
                throw new EPException("Cannot execute a fire-and-forget query that was compiled as module EPL, make sure to use the 'compileQuery' method of the compiler");
            }
            throw new EPException("Failed to find query provider class name in manifest (is this a compiled fire-and-forget query?)");
        }

        String className = compiled.getManifest().getQueryProviderClassName();

        // load module resource class
        Class clazz;
        try {
            clazz = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new EPException(e);
        }

        // get FAF provider
        FAFProvider fafProvider;
        try {
            fafProvider = (FAFProvider) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new EPException(e);
        }

        // initialize event types
        Map<String, EventType> moduleTypes = new HashMap<>();
        EventTypeResolverImpl eventTypeResolver = new EventTypeResolverImpl(moduleTypes, services.getEventTypePathRegistry(), services.getEventTypeRepositoryBus(), services.getBeanEventTypeFactoryPrivate());
        EventTypeCollectorImpl eventTypeCollector = new EventTypeCollectorImpl(moduleTypes, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeFactory(), services.getBeanEventTypeStemService(), eventTypeResolver, services.getXmlFragmentEventTypeFactory(), services.getEventTypeAvroHandler(), services.getEventBeanTypedEventFactory());
        fafProvider.initializeEventTypes(new EPModuleEventTypeInitServicesImpl(eventTypeCollector, eventTypeResolver));

        // initialize query
        fafProvider.initializeQuery(new EPStatementInitServicesImpl("faf-query", Collections.emptyMap(), null, null, eventTypeResolver, null, null, null, null, false, null, null, services));
        return fafProvider;
    }

    public static void validateSubstitutionParams(FAFQueryMethodProvider queryMethodProvider) {
        Class[] substitutionParamsTypes = queryMethodProvider.getQueryInformationals().getSubstitutionParamsTypes();
        if (substitutionParamsTypes != null && substitutionParamsTypes.length > 0) {
            throw new EPException("Missing values for substitution parameters, use prepare-parameterized instead");
        }
    }

    public static void checkSubstitutionSatisfied(EPFireAndForgetPreparedQueryParameterizedImpl impl) {
        if (impl.getUnsatisfiedParamsOneOffset().isEmpty()) {
            return;
        }
        int num = impl.getUnsatisfiedParamsOneOffset().iterator().next();
        if (impl.getNames() != null && !impl.getNames().isEmpty()) {
            String name = null;
            for (Map.Entry<String, Integer> entry : impl.getNames().entrySet()) {
                if (entry.getValue() == num) {
                    name = entry.getKey();
                    break;
                }
            }
            if (name != null) {
                throw new EPException("Missing value for substitution parameter '" + name + "'");
            }
        }
        throw new EPException("Missing value for substitution parameter " + num);
    }
}
