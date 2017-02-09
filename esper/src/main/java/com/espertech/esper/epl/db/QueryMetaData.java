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
package com.espertech.esper.epl.db;

import java.util.List;
import java.util.Map;

/**
 * Holder for query meta data information obtained from interrogating statements.
 */
public class QueryMetaData {
    private List<String> inputParameters;
    private Map<String, DBOutputTypeDesc> outputParameters;

    /**
     * Ctor.
     *
     * @param inputParameters  is the input parameter names
     * @param outputParameters is the output column names and types
     */
    public QueryMetaData(List<String> inputParameters, Map<String, DBOutputTypeDesc> outputParameters) {
        this.inputParameters = inputParameters;
        this.outputParameters = outputParameters;
    }

    /**
     * Return the input parameters.
     *
     * @return input parameter names
     */
    public List<String> getInputParameters() {
        return inputParameters;
    }

    /**
     * Returns a map of output column name and type descriptor.
     *
     * @return column names and types
     */
    public Map<String, DBOutputTypeDesc> getOutputParameters() {
        return outputParameters;
    }
}
