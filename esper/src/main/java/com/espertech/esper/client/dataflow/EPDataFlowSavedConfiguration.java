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
package com.espertech.esper.client.dataflow;

import java.io.Serializable;

/**
 * A data flow configuration is just a configuration name, a data flow name
 * and an instantiation options object.
 */
public class EPDataFlowSavedConfiguration implements Serializable {

    private static final long serialVersionUID = -7332342547494085356L;

    private final String savedConfigurationName;
    private final String dataflowName;
    private final EPDataFlowInstantiationOptions options;

    /**
     * Ctor.
     *
     * @param savedConfigurationName name of saved configuration
     * @param dataflowName           data flow name
     * @param options                options object
     */
    public EPDataFlowSavedConfiguration(String savedConfigurationName, String dataflowName, EPDataFlowInstantiationOptions options) {
        this.savedConfigurationName = savedConfigurationName;
        this.dataflowName = dataflowName;
        this.options = options;
    }

    /**
     * Configuation name.
     *
     * @return name
     */
    public String getSavedConfigurationName() {
        return savedConfigurationName;
    }

    /**
     * Data flow name.
     *
     * @return data flow name
     */
    public String getDataflowName() {
        return dataflowName;
    }

    /**
     * Data flow instantiation options.
     *
     * @return options
     */
    public EPDataFlowInstantiationOptions getOptions() {
        return options;
    }
}
