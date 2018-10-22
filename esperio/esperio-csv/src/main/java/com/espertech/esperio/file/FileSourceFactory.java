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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esperio.csv.AdapterInputSource;

import java.io.File;
import java.util.Map;

public class FileSourceFactory implements DataFlowOperatorFactory {

    private ExprEvaluator file;
    private ExprEvaluator classpathFile;
    private ExprEvaluator hasHeaderLine;
    private ExprEvaluator hasTitleLine;
    private Map<String, Object> adapterInputSource;
    private ExprEvaluator numLoops;
    private String[] propertyNames;
    private ExprEvaluator format;
    private ExprEvaluator propertyNameLine;
    private ExprEvaluator propertyNameFile;
    private ExprEvaluator dateFormat;
    private EventType outputEventType;
    private EventType[] outputPortTypes;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        boolean classpathFileFlag = DataFlowParameterResolution.resolveWithDefault("classpathFile", classpathFile, false, boolean.class, context);

        AdapterInputSource adapterInputSourceValue = DataFlowParameterResolution.resolveOptionalInstance("adapterInputSource", adapterInputSource, AdapterInputSource.class, context);
        String fileName = DataFlowParameterResolution.resolveWithDefault("file", file, null, String.class, context);

        if (adapterInputSourceValue == null) {
            if (fileName != null) {
                if (classpathFileFlag) {
                    adapterInputSourceValue = new AdapterInputSource(fileName);
                } else {
                    adapterInputSourceValue = new AdapterInputSource(new File(fileName));
                }
            } else {
                throw new EPException("Failed to find required parameter, either the file or the adapterInputSource parameter is required");
            }
        }

        String formatValue = DataFlowParameterResolution.resolveStringOptional("format", format, context);
        if (formatValue == null || formatValue.equals("csv")) {
            boolean hasHeaderLineFlag = DataFlowParameterResolution.resolveWithDefault("hasHeaderLine", hasHeaderLine, false, boolean.class, context);
            boolean hasTitleLineFlag = DataFlowParameterResolution.resolveWithDefault("hasTitleLine", hasTitleLine, false, boolean.class, context);
            Integer numLoopsValue = DataFlowParameterResolution.resolveWithDefault("numLoops", numLoops, null, Integer.class, context);
            String dateFormatValue = DataFlowParameterResolution.resolveStringOptional("dateFormat", dateFormat, context);
            return new FileSourceCSV(this, context, adapterInputSourceValue, hasHeaderLineFlag, hasTitleLineFlag, numLoopsValue, propertyNames, dateFormatValue);
        } else if (formatValue.equals("line")) {
            String propertyNameLineValue = DataFlowParameterResolution.resolveStringOptional("propertyNameLine", propertyNameLine, context);
            String propertyNameFileValue = DataFlowParameterResolution.resolveStringOptional("propertyNameFile", propertyNameFile, context);
            return new FileSourceLineUnformatted(this, context, adapterInputSourceValue, fileName, propertyNameLineValue, propertyNameFileValue);
        } else {
            throw new IllegalArgumentException("Unrecognized file format '" + formatValue + "'");
        }
    }

    public ExprEvaluator getFile() {
        return file;
    }

    public void setFile(ExprEvaluator file) {
        this.file = file;
    }

    public ExprEvaluator getClasspathFile() {
        return classpathFile;
    }

    public void setClasspathFile(ExprEvaluator classpathFile) {
        this.classpathFile = classpathFile;
    }

    public ExprEvaluator getHasHeaderLine() {
        return hasHeaderLine;
    }

    public void setHasHeaderLine(ExprEvaluator hasHeaderLine) {
        this.hasHeaderLine = hasHeaderLine;
    }

    public ExprEvaluator getHasTitleLine() {
        return hasTitleLine;
    }

    public void setHasTitleLine(ExprEvaluator hasTitleLine) {
        this.hasTitleLine = hasTitleLine;
    }

    public Map<String, Object> getAdapterInputSource() {
        return adapterInputSource;
    }

    public void setAdapterInputSource(Map<String, Object> adapterInputSource) {
        this.adapterInputSource = adapterInputSource;
    }

    public ExprEvaluator getNumLoops() {
        return numLoops;
    }

    public void setNumLoops(ExprEvaluator numLoops) {
        this.numLoops = numLoops;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    public ExprEvaluator getFormat() {
        return format;
    }

    public void setFormat(ExprEvaluator format) {
        this.format = format;
    }

    public ExprEvaluator getPropertyNameLine() {
        return propertyNameLine;
    }

    public void setPropertyNameLine(ExprEvaluator propertyNameLine) {
        this.propertyNameLine = propertyNameLine;
    }

    public ExprEvaluator getPropertyNameFile() {
        return propertyNameFile;
    }

    public void setPropertyNameFile(ExprEvaluator propertyNameFile) {
        this.propertyNameFile = propertyNameFile;
    }

    public ExprEvaluator getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(ExprEvaluator dateFormat) {
        this.dateFormat = dateFormat;
    }

    public EventType getOutputEventType() {
        return outputEventType;
    }

    public void setOutputEventType(EventType outputEventType) {
        this.outputEventType = outputEventType;
    }

    public EventType[] getOutputPortTypes() {
        return outputPortTypes;
    }

    public void setOutputPortTypes(EventType[] outputPortTypes) {
        this.outputPortTypes = outputPortTypes;
    }
}
