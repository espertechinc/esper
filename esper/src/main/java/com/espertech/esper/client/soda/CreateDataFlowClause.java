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
 * Represents a create-variable syntax for creating a new variable.
 */
public class CreateDataFlowClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String dataFlowName;
    private List<CreateSchemaClause> schemas;
    private List<DataFlowOperator> operators;

    /**
     * Ctor.
     */
    public CreateDataFlowClause() {
    }

    /**
     * Ctor.
     *
     * @param dataFlowName data flow name
     * @param schemas      schemas
     * @param operators    operators
     */
    public CreateDataFlowClause(String dataFlowName, List<CreateSchemaClause> schemas, List<DataFlowOperator> operators) {
        this.dataFlowName = dataFlowName;
        this.schemas = schemas;
        this.operators = operators;
    }

    /**
     * Returns the data flow name.
     *
     * @return name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }

    /**
     * Sets the data flow name.
     *
     * @param dataFlowName name
     */
    public void setDataFlowName(String dataFlowName) {
        this.dataFlowName = dataFlowName;
    }

    /**
     * Returns schemas.
     *
     * @return schemas
     */
    public List<CreateSchemaClause> getSchemas() {
        return schemas;
    }

    /**
     * Sets schemas.
     *
     * @param schemas schemas to set
     */
    public void setSchemas(List<CreateSchemaClause> schemas) {
        this.schemas = schemas;
    }

    /**
     * Returns operators.
     *
     * @return operator definitions
     */
    public List<DataFlowOperator> getOperators() {
        return operators;
    }

    /**
     * Sets operators.
     *
     * @param operators to define
     */
    public void setOperators(List<DataFlowOperator> operators) {
        this.operators = operators;
    }

    /**
     * Render as EPL.
     *
     * @param writer    to output to
     * @param formatter to use
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append("create dataflow ");
        writer.append(dataFlowName);
        if (schemas != null) {
            for (CreateSchemaClause clause : schemas) {
                formatter.beginDataFlowSchema(writer);
                clause.toEPL(writer);
                writer.append(",");
            }
        }
        if (operators != null) {
            formatter.beginDataFlowOperator(writer);
            for (DataFlowOperator clause : operators) {
                clause.toEPL(writer, formatter);
            }
        }
    }
}
