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

import java.io.Serializable;
import java.util.List;

public class CreateDataFlowDesc implements Serializable {
    private static final long serialVersionUID = -3919668393146597813L;
    private final String graphName;
    private final List<GraphOperatorSpec> operators;
    private final List<CreateSchemaDesc> schemas;

    public CreateDataFlowDesc(String graphName, List<GraphOperatorSpec> operators, List<CreateSchemaDesc> schemas) {
        this.graphName = graphName;
        this.operators = operators;
        this.schemas = schemas;
    }

    public String getGraphName() {
        return graphName;
    }

    public List<GraphOperatorSpec> getOperators() {
        return operators;
    }

    public List<CreateSchemaDesc> getSchemas() {
        return schemas;
    }
}
