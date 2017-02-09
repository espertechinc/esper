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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

/**
 * Specification for a view object consists of a namespace, name and view object parameters.
 */
public final class ViewSpec extends ObjectSpec {
    public final static ViewSpec[] EMPTY_VIEWSPEC_ARRAY = new ViewSpec[0];

    private static final long serialVersionUID = -2881179463072647071L;

    /**
     * Constructor.
     *
     * @param namespace      if the namespace the object is in
     * @param objectName     is the name of the object
     * @param viewParameters is a list of expressions representing the view parameters
     */
    public ViewSpec(String namespace, String objectName, List<ExprNode> viewParameters) {
        super(namespace, objectName, viewParameters);
    }

    public static ViewSpec[] toArray(List<ViewSpec> viewSpecs) {
        if (viewSpecs.isEmpty()) {
            return EMPTY_VIEWSPEC_ARRAY;
        }
        return viewSpecs.toArray(new ViewSpec[viewSpecs.size()]);
    }
}
