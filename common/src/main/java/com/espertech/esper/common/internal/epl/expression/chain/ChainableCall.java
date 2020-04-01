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
package com.espertech.esper.common.internal.epl.expression.chain;

import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery.acceptParams;

public class ChainableCall extends Chainable {
    private String name;
    private String nameUnescaped;
    private List<ExprNode> parameters;

    public ChainableCall(boolean distinct, boolean optional, String name, String nameUnescaped, List<ExprNode> parameters) {
        super(distinct, optional);
        this.name = name;
        this.nameUnescaped = nameUnescaped;
        this.parameters = parameters;
    }

    public ChainableCall(String name, List<ExprNode> parameters) {
        this.name = name;
        this.nameUnescaped = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(List<ExprNode> parameters) {
        this.parameters = parameters;
    }

    public String getNameUnescaped() {
        return nameUnescaped;
    }

    public List<ExprNode> getParameters() {
        return parameters;
    }

    public void addParametersTo(Collection<ExprNode> result) {
        result.addAll(parameters);
    }

    public void accept(ExprNodeVisitor visitor) {
        acceptParams(visitor, parameters);
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        acceptParams(visitor, parameters);
    }

    public void accept(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        acceptParams(visitor, parameters, parent);
    }

    public String getRootNameOrEmptyString() {
        return name;
    }

    public List<ExprNode> getParametersOrEmpty() {
        return parameters;
    }

    public void validateExpressions(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        validateExpressions(parameters, origin, validationContext);
    }

    public String toString() {
        return "ChainableCall{" +
            "name='" + name + '\'' +
            ", parameters=" + parameters +
            '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainableCall that = (ChainableCall) o;
        return super.equalsChainable(that) && name.equals(that.name) && ExprNodeUtilityCompare.deepEquals(parameters, that.parameters);
    }

    public int hashCode() {
        return name.hashCode();
    }
}
