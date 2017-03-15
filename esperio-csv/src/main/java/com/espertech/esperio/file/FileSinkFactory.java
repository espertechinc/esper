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
package com.espertech.esperio.file;

import com.espertech.esper.client.EPException;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.interfaces.DataFlowOperatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSinkFactory implements DataFlowOperatorFactory {

    private static final Logger log = LoggerFactory.getLogger(FileSinkFactory.class);

    @DataFlowOpParameter
    private String file;

    @DataFlowOpParameter
    private boolean classpathFile;

    @DataFlowOpParameter
    private boolean append;

    public Object create() {
        if (file == null) {
            throw new EPException("Failed to find required 'file' parameter");
        }
        return new FileSinkCSV(file, classpathFile, append);
    }
}
