/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view;

import com.espertech.esper.client.hook.VirtualDataWindowFactory;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.spec.PluggableObjectEntry;
import com.espertech.esper.epl.spec.PluggableObjectRegistry;
import com.espertech.esper.epl.spec.PluggableObjectType;
import com.espertech.esper.epl.virtualdw.VirtualDWViewFactory;
import com.espertech.esper.epl.virtualdw.VirtualDWViewFactoryImpl;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Resolves view namespace and name to view factory class, using configuration.
 */
public class ViewResolutionServiceImpl implements ViewResolutionService
{
    private static final Log log = LogFactory.getLog(ViewResolutionServiceImpl.class);

    private final PluggableObjectRegistry viewObjects;
    private final String optionalNamedWindowName;
    private final Class virtualDataWindowViewFactory;

    /**
     * Ctor.
     * @param viewObjects is the view objects to use for resolving views, can be both built-in and plug-in views.
     */
    public ViewResolutionServiceImpl(PluggableObjectRegistry viewObjects, String optionalNamedWindowName, Class virtualDataWindowViewFactory)
    {
        this.viewObjects = viewObjects;
        this.optionalNamedWindowName = optionalNamedWindowName;
        this.virtualDataWindowViewFactory = virtualDataWindowViewFactory;
    }

    public ViewFactory create(String nameSpace, String name) throws ViewProcessingException
    {
        if (log.isDebugEnabled())
        {
            log.debug(".create Creating view factory, namespace=" + nameSpace + " name=" + name);
        }

        Class viewFactoryClass = null;

        Pair<Class, PluggableObjectEntry> pair = viewObjects.lookup(nameSpace, name);
        if (pair != null)
        {
            if (pair.getSecond().getType() == PluggableObjectType.VIEW )
            {
                // Handle named windows in a configuration that always declares a system-wide virtual view factory
                if (optionalNamedWindowName != null && virtualDataWindowViewFactory != null) {
                    return new VirtualDWViewFactoryImpl(virtualDataWindowViewFactory, optionalNamedWindowName, null);
                }

                viewFactoryClass = pair.getFirst();
            }
            else if (pair.getSecond().getType() == PluggableObjectType.VIRTUALDW)
            {
                if (optionalNamedWindowName == null) {
                    throw new ViewProcessingException("Virtual data window requires use with a named window in the create-window syntax");
                }
                return new VirtualDWViewFactoryImpl(pair.getFirst(), optionalNamedWindowName, pair.getSecond().getCustomConfigs());
            }
            else
            {
                throw new ViewProcessingException("Invalid object type '" + pair.getSecond() + "' for view '" + name + "'");
            }
        }

        if (viewFactoryClass == null)
        {
            String message = "View name '" + nameSpace + ":" + name + "' is not a known view name";
            throw new ViewProcessingException(message);
        }

        ViewFactory viewFactory;
        try
        {
            viewFactory = (ViewFactory) viewFactoryClass.newInstance();

            if (log.isDebugEnabled())
            {
                log.debug(".create Successfully instantiated view");
            }
        }
        catch (ClassCastException e)
        {
            String message = "Error casting view factory instance to " + ViewFactory.class.getName() + " interface for view '" + name + "'";
            throw new ViewProcessingException(message, e);
        }
        catch (IllegalAccessException e)
        {
            String message = "Error invoking view factory constructor for view '" + name;
            message += "', no invocation access for Class.newInstance";
            throw new ViewProcessingException(message, e);
        }
        catch (InstantiationException e)
        {
            String message = "Error invoking view factory constructor for view '" + name;
            message += "' using Class.newInstance";
            throw new ViewProcessingException(message, e);
        }

        return viewFactory;
    }
}
