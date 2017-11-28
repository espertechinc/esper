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
package com.espertech.esper.supportunit.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.StreamSpecOptions;
import com.espertech.esper.epl.spec.ViewSpec;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewFactoryChain;
import com.espertech.esper.view.ViewServiceImpl;

import java.util.LinkedList;
import java.util.List;

/**
 * Convenience class for making view specifications from class and string arrays.
 */
public class SupportViewSpecFactory {
    public static List<ViewSpec> makeSpecListOne() throws Exception {
        List<ViewSpec> specifications = new LinkedList<ViewSpec>();

        ViewSpec specOne = makeSpec("win", "length",
                new Class[]{Integer.class}, new String[]{"1000"});
        ViewSpec specTwo = makeSpec("stat", "uni",
                new Class[]{String.class}, new String[]{"intPrimitive"});
        ViewSpec specThree = makeSpec("std", "lastevent", null, null);

        specifications.add(specOne);
        specifications.add(specTwo);
        specifications.add(specThree);

        return specifications;
    }

    public static List<ViewFactory> makeFactoryListOne(EventType parentEventType) throws Exception {
        return makeFactories(parentEventType, makeSpecListOne());
    }

    public static List<ViewSpec> makeSpecListTwo() throws Exception {
        List<ViewSpec> specifications = new LinkedList<ViewSpec>();

        ViewSpec specTwo = makeSpec("win", "length",
                new Class[]{int.class}, new String[]{"100"});
        specifications.add(specTwo);

        return specifications;
    }

    public static List<ViewFactory> makeFactoryListTwo(EventType parentEventType) throws Exception {
        return makeFactories(parentEventType, makeSpecListTwo());
    }

    public static List<ViewSpec> makeSpecListThree() throws Exception {
        List<ViewSpec> specifications = new LinkedList<ViewSpec>();

        ViewSpec specOne = SupportViewSpecFactory.makeSpec("win", "length",
                new Class[]{Integer.class}, new String[]{"1000"});
        ViewSpec specTwo = SupportViewSpecFactory.makeSpec("std", "unique",
                new Class[]{String.class}, new String[]{"theString"});

        specifications.add(specOne);
        specifications.add(specTwo);

        return specifications;
    }

    public static List<ViewFactory> makeFactoryListThree(EventType parentEventType) throws Exception {
        return makeFactories(parentEventType, makeSpecListThree());
    }

    public static List<ViewSpec> makeSpecListFour() throws Exception {
        List<ViewSpec> specifications = new LinkedList<ViewSpec>();

        ViewSpec specOne = SupportViewSpecFactory.makeSpec("win", "length",
                new Class[]{Integer.class}, new String[]{"1000"});
        ViewSpec specTwo = SupportViewSpecFactory.makeSpec("stat", "uni",
                new Class[]{String.class}, new String[]{"intPrimitive"});
        ViewSpec specThree = SupportViewSpecFactory.makeSpec("std", "size", null, null);

        specifications.add(specOne);
        specifications.add(specTwo);
        specifications.add(specThree);

        return specifications;
    }

    public static List<ViewFactory> makeFactoryListFour(EventType parentEventType) throws Exception {
        return makeFactories(parentEventType, makeSpecListFour());
    }

    public static List<ViewSpec> makeSpecListFive() throws Exception {
        List<ViewSpec> specifications = new LinkedList<ViewSpec>();

        ViewSpec specOne = makeSpec("win", "time",
                new Class[]{Integer.class}, new String[]{"10000"});
        specifications.add(specOne);

        return specifications;
    }

    public static List<ViewFactory> makeFactoryListFive(EventType parentEventType) throws Exception {
        return makeFactories(parentEventType, makeSpecListFive());
    }

    public static ViewSpec makeSpec(String namespace, String viewName, Class[] paramTypes, String[] paramValues) throws Exception {
        return new ViewSpec(namespace, viewName, makeParams(paramTypes, paramValues));
    }

    private static LinkedList<ExprNode> makeParams(Class clazz[], String[] values) throws Exception {
        LinkedList<ExprNode> parameters = new LinkedList<ExprNode>();
        if (values == null) {
            return parameters;
        }

        for (int i = 0; i < values.length; i++) {
            ExprNode node;
            String value = values[i];
            if (clazz[i] == String.class) {
                if (value.startsWith("\"")) {
                    value = value.replace("\"", "");
                    node = new ExprConstantNodeImpl(value);
                } else {
                    node = SupportExprNodeFactory.makeIdentNodeBean(value);
                }
            } else if (clazz[i] == Boolean.class) {
                node = new ExprConstantNodeImpl(Boolean.valueOf(value));
            } else {
                node = new ExprConstantNodeImpl(Integer.valueOf(value));
            }
            parameters.add(node);
        }

        return parameters;
    }

    private static List<ViewFactory> makeFactories(EventType parentEventType, List<ViewSpec> viewSpecs) throws Exception {
        ViewServiceImpl svc = new ViewServiceImpl();
        ViewFactoryChain viewFactories = svc.createFactories(1, parentEventType, ViewSpec.toArray(viewSpecs), StreamSpecOptions.DEFAULT, SupportStatementContextFactory.makeContext(), false, -1);
        return viewFactories.getViewFactoryChain();
    }
}
