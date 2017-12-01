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
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for the update clause.
 */
public class UpdateClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String eventType;
    private String optionalAsClauseStreamName;
    private List<Assignment> assignments;
    private Expression optionalWhereClause;

    /**
     * Ctor.
     */
    public UpdateClause() {
    }

    /**
     * Ctor.
     *
     * @param eventType  the name of the type to update
     * @param expression expression returning a value to write
     * @return update clause
     */
    public static UpdateClause create(String eventType, Expression expression) {
        UpdateClause clause = new UpdateClause(eventType, null);
        clause.addAssignment(expression);
        return clause;
    }

    /**
     * Ctor.
     *
     * @param eventType                  the name of the type to update
     * @param optionalAsClauseStreamName as-clause for update, if any
     */
    public UpdateClause(String eventType, String optionalAsClauseStreamName) {
        this.eventType = eventType;
        this.optionalAsClauseStreamName = optionalAsClauseStreamName;
        assignments = new ArrayList<Assignment>();
    }

    /**
     * Adds a property to set to the clause.
     *
     * @param expression expression providing the new property value
     * @return clause
     */
    public UpdateClause addAssignment(Expression expression) {
        assignments.add(new Assignment(expression));
        return this;
    }

    /**
     * Returns the list of property assignments.
     *
     * @return pair of property name and expression
     */
    public List<Assignment> getAssignments() {
        return assignments;
    }

    /**
     * Sets a list of property assignments.
     *
     * @param assignments list of pairs of property name and expression
     */
    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    /**
     * Returns the name of the event type to update.
     *
     * @return name of type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Returns the name of the event type to update.
     *
     * @param eventType name of type
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * Returns the where-clause if any.
     *
     * @return where clause
     */
    public Expression getOptionalWhereClause() {
        return optionalWhereClause;
    }

    /**
     * Sets the where-clause if any.
     *
     * @param optionalWhereClause where clause
     */
    public void setOptionalWhereClause(Expression optionalWhereClause) {
        this.optionalWhereClause = optionalWhereClause;
    }

    /**
     * Returns the stream name.
     *
     * @return stream name
     */
    public String getOptionalAsClauseStreamName() {
        return optionalAsClauseStreamName;
    }

    /**
     * Returns the stream name.
     *
     * @param optionalAsClauseStreamName stream name
     */
    public void setOptionalAsClauseStreamName(String optionalAsClauseStreamName) {
        this.optionalAsClauseStreamName = optionalAsClauseStreamName;
    }

    /**
     * Renders the clause in EPL.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write("update istream ");
        writer.write(eventType);
        if (this.optionalAsClauseStreamName != null) {
            writer.write(" as ");
            writer.write(optionalAsClauseStreamName);
        }
        writer.write(" ");
        renderEPLAssignments(writer, assignments);

        if (optionalWhereClause != null) {
            writer.write(" where ");
            optionalWhereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }

    /**
     * Write assignments.
     *
     * @param writer      to write to
     * @param assignments to write
     */
    public static void renderEPLAssignments(StringWriter writer, List<Assignment> assignments) {
        writer.write("set ");
        String delimiter = "";
        for (Assignment pair : assignments) {
            writer.write(delimiter);
            pair.getValue().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ", ";
        }
    }
}
