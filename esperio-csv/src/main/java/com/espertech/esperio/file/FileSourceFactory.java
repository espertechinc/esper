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
import com.espertech.esper.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esperio.csv.AdapterInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileSourceFactory implements DataFlowOperatorFactory {

    private static final Logger log = LoggerFactory.getLogger(FileSourceFactory.class);

    @DataFlowOpParameter
    private String file;

    @DataFlowOpParameter
    private boolean classpathFile;

    @DataFlowOpParameter
    private boolean hasHeaderLine;

    @DataFlowOpParameter
    private boolean hasTitleLine;

    @DataFlowOpParameter
    private AdapterInputSource adapterInputSource;

    @DataFlowOpParameter
    private Integer numLoops;

    @DataFlowOpParameter
    private String[] propertyNames;

    @DataFlowOpParameter
    private String format;

    @DataFlowOpParameter
    private String propertyNameLine;

    @DataFlowOpParameter
    private String propertyNameFile;

    @DataFlowOpParameter
    private String dateFormat;

    public DataFlowSourceOperator create() {

        AdapterInputSource inputSource;
        if (adapterInputSource != null) {
            inputSource = adapterInputSource;
        } else if (file != null) {
            if (classpathFile) {
                inputSource = new AdapterInputSource(file);
            } else {
                inputSource = new AdapterInputSource(new File(file));
            }
        } else {
            throw new EPException("Failed to find required parameter, either the file or the adapterInputSource parameter is required");
        }

        if (format == null || format.equals("csv")) {
            return new FileSourceCSV(inputSource, hasHeaderLine, hasTitleLine, numLoops, propertyNames, dateFormat);
        } else if (format.equals("line")) {
            return new FileSourceLineUnformatted(inputSource, file, propertyNameLine, propertyNameFile);
        } else {
            throw new IllegalArgumentException("Unrecognized file format '" + format + "'");
        }
    }
}
