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

import com.espertech.esper.type.OuterJoinType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Qualifies a join by providing the outer join type (full/left/right) and joined-on properties.
 */
public class OuterJoinQualifier implements Serializable {
    private static final long serialVersionUID = 0L;

    private OuterJoinType type;
    private Expression left;
    private Expression right;
    private List<PropertyValueExpressionPair> additionalProperties;

    /**
     * Ctor.
     */
    public OuterJoinQualifier() {
    }

    /**
     * Creates qualifier.
     *
     * @param propertyLeft  is a property name providing joined-on values
     * @param type          is the type of outer join
     * @param propertyRight is a property name providing joined-on values
     * @return qualifier
     */
    public static OuterJoinQualifier create(String propertyLeft, OuterJoinType type, String propertyRight) {
        return new OuterJoinQualifier(type, new PropertyValueExpression(propertyLeft), new PropertyValueExpression(propertyRight));
    }

    /**
     * Ctor.
     *
     * @param left  is a property providing joined-on values
     * @param type  is the type of outer join
     * @param right is a property providing joined-on values
     */
    public OuterJoinQualifier(OuterJoinType type, PropertyValueExpression left, PropertyValueExpression right) {
        this(type, left, right, new ArrayList<PropertyValueExpressionPair>());
    }

    /**
     * Ctor.
     *
     * @param left                 is a property providing joined-on values
     * @param type                 is the type of outer join
     * @param right                is a property providing joined-on values
     * @param additionalProperties for any pairs of additional on-clause properties
     */
    public OuterJoinQualifier(OuterJoinType type, PropertyValueExpression left, PropertyValueExpression right, ArrayList<PropertyValueExpressionPair> additionalProperties) {
        this.type = type;
        this.left = left;
        this.right = right;
        this.additionalProperties = additionalProperties;
    }

    /**
     * Returns the type of outer join.
     *
     * @return outer join type
     */
    public OuterJoinType getType() {
        return type;
    }

    /**
     * Sets the type of outer join.
     *
     * @param type is the outer join type
     */
    public void setType(OuterJoinType type) {
        this.type = type;
    }

    /**
     * Set join properties.
     *
     * @param additionalProperties for outer join
     */
    public void setAdditionalProperties(List<PropertyValueExpressionPair> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    /**
     * Returns property value expression to join on.
     *
     * @return expression providing joined-on values
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Sets the property value expression to join on.
     *
     * @param left expression providing joined-on values
     */
    public void setLeft(Expression left) {
        this.left = left;
    }

    /**
     * Returns property value expression to join on.
     *
     * @return expression providing joined-on values
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Sets the property value expression to join on.
     *
     * @param right expression providing joined-on values
     */
    public void setRight(Expression right) {
        this.right = right;
    }

    /**
     * Add additional properties to the on-clause, which are logical-and to existing properties
     *
     * @param propertyLeft  property providing joined-on value
     * @param propertyRight property providing joined-on value
     * @return outer join qualifier
     */
    public OuterJoinQualifier add(String propertyLeft, String propertyRight) {
        additionalProperties.add(new PropertyValueExpressionPair(new PropertyValueExpression(propertyLeft), new PropertyValueExpression(propertyRight)));
        return this;
    }

    /**
     * Returns optional additional properties in the on-clause of the outer join.
     *
     * @return pairs of properties connected via logical-and in an on-clause
     */
    public List<PropertyValueExpressionPair> getAdditionalProperties() {
        return additionalProperties;
    }
}
