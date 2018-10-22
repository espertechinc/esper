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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardForge;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverFactory;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Resolves pattern object namespace and name to guard or observer factory class, using configuration.
 */
public class PatternObjectResolutionServiceImpl implements PatternObjectResolutionService {
    private static final Logger log = LoggerFactory.getLogger(PatternObjectResolutionServiceImpl.class);

    private final PluggableObjectCollection patternObjects;

    /**
     * Ctor.
     *
     * @param patternObjects is the pattern plug-in objects configured
     */
    public PatternObjectResolutionServiceImpl(PluggableObjectCollection patternObjects) {
        this.patternObjects = patternObjects;
    }

    public ObserverForge create(PatternObserverSpec spec) throws PatternObjectException {
        Object result = createForge(spec, PluggableObjectType.PATTERN_OBSERVER);
        ObserverForge forge;
        try {
            forge = (ObserverForge) result;

            if (log.isDebugEnabled()) {
                log.debug(".create Successfully instantiated observer");
            }
        } catch (ClassCastException e) {
            String message = "Error casting observer factory instance to " + ObserverFactory.class.getName() + " interface for observer '" + spec.getObjectName() + "'";
            throw new PatternObjectException(message, e);
        }
        return forge;
    }

    public GuardForge create(PatternGuardSpec spec) throws PatternObjectException {
        Object result = createForge(spec, PluggableObjectType.PATTERN_GUARD);
        GuardForge forge;
        try {
            forge = (GuardForge) result;

            if (log.isDebugEnabled()) {
                log.debug(".create Successfully instantiated guard");
            }
        } catch (ClassCastException e) {
            String message = "Error casting guard forge instance to " + GuardForge.class.getName() + " interface for guard '" + spec.getObjectName() + "'";
            throw new PatternObjectException(message, e);
        }
        return forge;
    }

    private Object createForge(ObjectSpec spec, PluggableObjectType type) throws PatternObjectException {
        if (log.isDebugEnabled()) {
            log.debug(".create Creating factory, spec=" + spec.toString());
        }

        // Find the factory class for this pattern object
        Class forgeClass = null;

        Map<String, Pair<Class, PluggableObjectEntry>> namespaceMap = patternObjects.getPluggables().get(spec.getObjectNamespace());
        if (namespaceMap != null) {
            Pair<Class, PluggableObjectEntry> pair = namespaceMap.get(spec.getObjectName());
            if (pair != null) {
                if (pair.getSecond().getType() == type) {
                    forgeClass = pair.getFirst();
                } else {
                    // invalid type: expecting observer, got guard
                    if (type == PluggableObjectType.PATTERN_GUARD) {
                        throw new PatternObjectException("Pattern observer function '" + spec.getObjectName() + "' cannot be used as a pattern guard");
                    } else {
                        throw new PatternObjectException("Pattern guard function '" + spec.getObjectName() + "' cannot be used as a pattern observer");
                    }
                }
            }
        }

        if (forgeClass == null) {
            if (type == PluggableObjectType.PATTERN_GUARD) {
                String message = "Pattern guard name '" + spec.getObjectName() + "' is not a known pattern object name";
                throw new PatternObjectException(message);
            } else if (type == PluggableObjectType.PATTERN_OBSERVER) {
                String message = "Pattern observer name '" + spec.getObjectName() + "' is not a known pattern object name";
                throw new PatternObjectException(message);
            } else {
                throw new PatternObjectException("Pattern object type '" + type + "' not known");
            }
        }

        Object result;
        try {
            result = forgeClass.newInstance();
        } catch (IllegalAccessException e) {
            String message = "Error invoking pattern object factory constructor for object '" + spec.getObjectName();
            message += "', no invocation access for Class.newInstance";
            throw new PatternObjectException(message, e);
        } catch (InstantiationException e) {
            String message = "Error invoking pattern object factory constructor for object '" + spec.getObjectName();
            message += "' using Class.newInstance";
            throw new PatternObjectException(message, e);
        }

        return result;
    }
}
