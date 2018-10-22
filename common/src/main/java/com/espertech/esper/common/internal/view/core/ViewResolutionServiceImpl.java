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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.PluggableObjectEntry;
import com.espertech.esper.common.internal.compile.stage1.spec.PluggableObjectRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.PluggableObjectType;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactoryForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves view namespace and name to view factory class, using configuration.
 */
public class ViewResolutionServiceImpl implements ViewResolutionService {
    private static final Logger log = LoggerFactory.getLogger(ViewResolutionServiceImpl.class);

    private final PluggableObjectRegistry viewObjects;

    public ViewResolutionServiceImpl(PluggableObjectRegistry viewObjects) {
        this.viewObjects = viewObjects;
    }

    public ViewFactoryForge create(String nameSpace, String name, String optionalCreateNamedWindowName) throws ViewProcessingException {
        if (log.isDebugEnabled()) {
            log.debug(".create Creating view factory, namespace=" + nameSpace + " name=" + name);
        }

        Class viewFactoryClass = null;

        Pair<Class, PluggableObjectEntry> pair = viewObjects.lookup(nameSpace, name);
        if (pair != null) {
            if (pair.getSecond().getType() == PluggableObjectType.VIEW) {
                viewFactoryClass = pair.getFirst();
            } else if (pair.getSecond().getType() == PluggableObjectType.VIRTUALDW) {
                if (optionalCreateNamedWindowName == null) {
                    throw new ViewProcessingException("Virtual data window requires use with a named window in the create-window syntax");
                }
                return new VirtualDWViewFactoryForge(pair.getFirst(), optionalCreateNamedWindowName, pair.getSecond().getCustomConfigs());
            } else {
                throw new ViewProcessingException("Invalid object type '" + pair.getSecond() + "' for view '" + name + "'");
            }
        }

        if (viewFactoryClass == null) {
            String message = nameSpace == null ?
                    "View name '" + name + "' is not a known view name" :
                    "View name '" + nameSpace + ":" + name + "' is not a known view name";
            throw new ViewProcessingException(message);
        }

        ViewFactoryForge forge;
        try {
            forge = (ViewFactoryForge) viewFactoryClass.newInstance();

            if (log.isDebugEnabled()) {
                log.debug(".create Successfully instantiated view");
            }
        } catch (ClassCastException e) {
            String message = "Error casting view factory instance to " + ViewFactoryForge.class.getName() + " interface for view '" + name + "'";
            throw new ViewProcessingException(message, e);
        } catch (IllegalAccessException e) {
            String message = "Error invoking view factory constructor for view '" + name;
            message += "', no invocation access for Class.newInstance";
            throw new ViewProcessingException(message, e);
        } catch (InstantiationException e) {
            String message = "Error invoking view factory constructor for view '" + name;
            message += "' using Class.newInstance";
            throw new ViewProcessingException(message, e);
        }

        return forge;
    }
}
