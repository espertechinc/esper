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
import java.util.List;

/**
 * Represents an input port of an operator.
 */
public class DataFlowOperatorInput implements Serializable {

    private static final long serialVersionUID = -6664793568622110220L;
    private List<String> inputStreamNames;
    private String optionalAsName;

    /**
     * Ctor.
     */
    public DataFlowOperatorInput() {
    }

    /**
     * Ctor.
     *
     * @param inputStreamNames names of input streams for the same port
     * @param optionalAsName   optional alias
     */
    public DataFlowOperatorInput(List<String> inputStreamNames, String optionalAsName) {
        this.inputStreamNames = inputStreamNames;
        this.optionalAsName = optionalAsName;
    }

    /**
     * Returns the input stream names.
     *
     * @return input stream names
     */
    public List<String> getInputStreamNames() {
        return inputStreamNames;
    }

    /**
     * Sets the input stream names.
     *
     * @param inputStreamNames input stream names
     */
    public void setInputStreamNames(List<String> inputStreamNames) {
        this.inputStreamNames = inputStreamNames;
    }

    /**
     * Returns the alias name.
     *
     * @return alias
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    /**
     * Sets the alias name.
     *
     * @param optionalAsName alias to set
     */
    public void setOptionalAsName(String optionalAsName) {
        this.optionalAsName = optionalAsName;
    }
}
