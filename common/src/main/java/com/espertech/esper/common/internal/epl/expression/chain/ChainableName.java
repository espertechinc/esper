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

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChainableName extends Chainable {
    private final String name;
    private final String nameUnescaped;

    public ChainableName(boolean distinct, boolean optional, String name) {
        super(distinct, optional);
        this.name = name;
        this.nameUnescaped = name;
    }

    public ChainableName(boolean distinct, boolean optional, String name, String nameUnescaped) {
        super(distinct, optional);
        this.name = name;
        this.nameUnescaped = nameUnescaped;
    }

    public ChainableName(String name) {
        this.name = name;
        this.nameUnescaped = name;
    }

    public String getName() {
        return name;
    }

    public String getNameUnescaped() {
        return nameUnescaped;
    }

    public void addParametersTo(Collection<ExprNode> result) {
        // no parameters
    }

    public void accept(ExprNodeVisitor visitor) {
        // no parameters
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        // no parameters
    }

    public void accept(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        // no parameters
    }

    public String getRootNameOrEmptyString() {
        return name;
    }

    public List<ExprNode> getParametersOrEmpty() {
        return Collections.emptyList();
    }

    public void validateExpressions(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        // no action
    }

    public String toString() {
        return "ChainableName{" +
            "name='" + name + '\'' +
            '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainableName that = (ChainableName) o;
        return super.equalsChainable(that) && name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }
}
