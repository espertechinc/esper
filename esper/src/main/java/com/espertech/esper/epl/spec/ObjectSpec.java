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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.List;

/**
 * Encapsulates the information required to specify an object identification and construction.
 * <p>
 * Abstract class for use with any object, such as views, pattern guards or pattern observers.
 * <p>
 * A object construction specification can be equal to another specification. This information can be
 * important to determine reuse of any object.
 */
public abstract class ObjectSpec implements Serializable {
    private final String objectNamespace;
    private final String objectName;
    private final List<ExprNode> objectParameters;
    private static final long serialVersionUID = 8376856305427395086L;

    /**
     * Constructor.
     *
     * @param namespace        if the namespace the object is in
     * @param objectName       is the name of the object
     * @param objectParameters is a list of values representing the object parameters
     */
    public ObjectSpec(String namespace, String objectName, List<ExprNode> objectParameters) {
        this.objectNamespace = namespace;
        this.objectName = objectName;
        this.objectParameters = objectParameters;
    }

    /**
     * Returns namespace for view object.
     *
     * @return namespace
     */
    public String getObjectNamespace() {
        return objectNamespace;
    }

    /**
     * Returns the object name.
     *
     * @return object name
     */
    public final String getObjectName() {
        return objectName;
    }

    /**
     * Returns the list of object parameters.
     *
     * @return list of expressions representing object parameters
     */
    public final List<ExprNode> getObjectParameters() {
        return objectParameters;
    }

    public final boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        if (otherObject == null) {
            return false;
        }

        if (getClass() != otherObject.getClass()) {
            return false;
        }

        final ObjectSpec other = (ObjectSpec) otherObject;
        if (!(this.objectName).equals(other.objectName)) {
            return false;
        }

        if (objectParameters.size() != other.objectParameters.size()) {
            return false;
        }

        // Compare object parameter by object parameter
        int index = 0;
        for (ExprNode thisParam : objectParameters) {
            ExprNode otherParam = other.objectParameters.get(index);
            index++;

            if (!ExprNodeUtilityCore.deepEquals(thisParam, otherParam, false)) {
                return false;
            }
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = objectNamespace.hashCode();
        result = 31 * result + objectName.hashCode();
        return result;
    }

    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("objectName=");
        buffer.append(objectName);
        buffer.append("  objectParameters=(");
        char delimiter = ' ';

        if (objectParameters != null) {
            for (ExprNode param : objectParameters) {
                buffer.append(delimiter);
                buffer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(param));
                delimiter = ',';
            }
        }

        buffer.append(')');

        return buffer.toString();
    }
}
