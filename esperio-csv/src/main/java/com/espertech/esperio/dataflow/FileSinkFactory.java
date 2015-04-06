/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esperio.dataflow;

import com.espertech.esper.client.EPException;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.interfaces.DataFlowOperatorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileSinkFactory implements DataFlowOperatorFactory {

    private static final Log log = LogFactory.getLog(FileSinkFactory.class);

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
