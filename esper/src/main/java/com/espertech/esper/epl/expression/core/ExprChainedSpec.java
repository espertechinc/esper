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
package com.espertech.esper.epl.expression.core;

import java.io.Serializable;
import java.util.List;

public class ExprChainedSpec implements Serializable {
    private static final long serialVersionUID = -5618484186038424466L;

    private String name;
    private List<ExprNode> parameters;
    private boolean property;

    public ExprChainedSpec(String name, List<ExprNode> parameters, boolean property) {
        this.name = name;
        this.parameters = parameters;
        this.property = property;
    }

    public String getName() {
        return name;
    }

    public List<ExprNode> getParameters() {
        return parameters;
    }

    public boolean isProperty() {
        return property;
    }

    public void setParameters(List<ExprNode> parameters) {
        this.parameters = parameters;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExprChainedSpec that = (ExprChainedSpec) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return ExprNodeUtilityCore.deepEquals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ExprChainedSpec{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
