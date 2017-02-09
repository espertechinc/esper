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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Base class for named engine objects such as views, patterns guards and observers.
 */
public abstract class EPBaseNamedObject implements Serializable {
    private static final long serialVersionUID = 0L;

    private String namespace;
    private String name;
    private List<Expression> parameters;

    /**
     * Ctor.
     */
    public EPBaseNamedObject() {
    }

    /**
     * Ctor.
     *
     * @param namespace  is the namespace of the object, i.e. view namespace or pattern object namespace
     * @param name       is the name of the object, such as the view name
     * @param parameters is the optional parameters to the view or pattern object, or empty list for no parameters
     */
    public EPBaseNamedObject(String namespace, String name, List<Expression> parameters) {
        this.namespace = namespace;
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * Returns the object namespace name.
     *
     * @return namespace name
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the object namespace name
     *
     * @param namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the object name.
     *
     * @return object name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the object name.
     *
     * @param name is the object name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the object parameters.
     *
     * @return parameters for object, empty list for no parameters
     */
    public List<Expression> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters for the object.
     *
     * @param parameters parameters for object, empty list for no parameters
     */
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    /**
     * Writes the object in EPL-syntax in the format "namespace:name(parameter, parameter, ..., parameter)"
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write(namespace);
        writer.write(':');
        writer.write(name);
        writer.write('(');
        ExpressionBase.toPrecedenceFreeEPL(getParameters(), writer);
        writer.write(')');
    }
}
