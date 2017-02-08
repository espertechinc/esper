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
package com.espertech.esper.view;

import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Method;

public class ViewFactoryProxy implements java.lang.reflect.InvocationHandler {

    private static Method target = JavaClassHelper.getMethodByName(ViewFactory.class, "makeView");

    private String engineURI;
    private String statementName;
    private ViewFactory viewFactory;
    private String viewName;

    public static Object newInstance(String engineURI, String statementName, ViewFactory viewFactory, String viewName) {
        return java.lang.reflect.Proxy.newProxyInstance(
                viewFactory.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(viewFactory.getClass()),
                new ViewFactoryProxy(engineURI, statementName, viewFactory, viewName));
    }

    public ViewFactoryProxy(String engineURI, String statementName, ViewFactory viewFactory, String viewName) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.viewFactory = viewFactory;
        this.viewName = viewName;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        if (!m.equals(target)) {
            return m.invoke(viewFactory, args);
        }

        View view = (View) m.invoke(viewFactory, args);
        return ViewProxy.newInstance(engineURI, statementName, viewName, view);
    }
}

