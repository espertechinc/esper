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
package com.espertech.esper.client.soda;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A view provides a projection upon a stream, such as a data window, grouping or unique.
 * For views, the namespace is an optional value and can be null for any-namespace.
 */
public class View extends EPBaseNamedObject {
    private static final long serialVersionUID = 704960216123401420L;

    /**
     * Ctor.
     */
    public View() {
    }

    /**
     * Creates a view.
     *
     * @param namespace is thie view namespace, i.e. "win" for data windows
     * @param name      is the view name, i.e. "length" for length window
     * @return view
     */
    public static View create(String namespace, String name) {
        return new View(namespace, name, new ArrayList<Expression>());
    }

    /**
     * Creates a view.
     *
     * @param name is the view name, i.e. "length" for length window
     * @return view
     */
    public static View create(String name) {
        return new View(null, name, new ArrayList<Expression>());
    }

    /**
     * Creates a view.
     *
     * @param namespace  is thie view namespace, i.e. "win" for data windows
     * @param name       is the view name, i.e. "length" for length window
     * @param parameters is a list of view parameters, or empty if there are no parameters for the view
     * @return view
     */
    public static View create(String namespace, String name, List<Expression> parameters) {
        return new View(namespace, name, parameters);
    }

    /**
     * Creates a view.
     *
     * @param name       is the view name, i.e. "length" for length window
     * @param parameters is a list of view parameters, or empty if there are no parameters for the view
     * @return view
     */
    public static View create(String name, List<Expression> parameters) {
        return new View(null, name, parameters);
    }

    /**
     * Creates a view.
     *
     * @param namespace  is thie view namespace, i.e. "win" for data windows
     * @param name       is the view name, i.e. "length" for length window
     * @param parameters is a list of view parameters, or empty if there are no parameters for the view
     * @return view
     */
    public static View create(String namespace, String name, Expression... parameters) {
        if (parameters != null) {
            return new View(namespace, name, Arrays.asList(parameters));
        } else {
            return new View(namespace, name, new ArrayList<Expression>());
        }
    }

    /**
     * Creates a view.
     *
     * @param name       is the view name, i.e. "length" for length window
     * @param parameters is a list of view parameters, or empty if there are no parameters for the view
     * @return view
     */
    public static View create(String name, Expression... parameters) {
        if (parameters != null) {
            return new View(null, name, Arrays.asList(parameters));
        } else {
            return new View(null, name, new ArrayList<Expression>());
        }
    }

    /**
     * Creates a view.
     *
     * @param namespace  is thie view namespace, i.e. "win" for data windows
     * @param name       is the view name, i.e. "length" for length window
     * @param parameters is a list of view parameters, or empty if there are no parameters for the view
     */
    public View(String namespace, String name, List<Expression> parameters) {
        super(namespace, name, parameters);
    }

    /**
     * Render view.
     *
     * @param writer to render to
     */
    public void toEPLWithHash(StringWriter writer) {
        writer.write(getName());
        if (!getParameters().isEmpty()) {
            writer.write('(');
            ExpressionBase.toPrecedenceFreeEPL(getParameters(), writer);
            writer.write(')');
        }
    }
}
