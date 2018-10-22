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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class FileSinkFactory implements DataFlowOperatorFactory {

    private ExprEvaluator file;
    private ExprEvaluator classpathFile;
    private ExprEvaluator append;
    private EventType eventType;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        String fileName = DataFlowParameterResolution.resolveStringRequired("file", file, context);
        boolean classPathFileFlag = DataFlowParameterResolution.resolveWithDefault("classpathFile", classpathFile, false, boolean.class, context);
        boolean appendFlag = DataFlowParameterResolution.resolveWithDefault("append", append, false, boolean.class, context);
        return new FileSinkCSV(this, fileName, classPathFileFlag, appendFlag);
    }

    public void setFile(ExprEvaluator file) {
        this.file = file;
    }

    public void setClasspathFile(ExprEvaluator classpathFile) {
        this.classpathFile = classpathFile;
    }

    public void setAppend(ExprEvaluator append) {
        this.append = append;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ExprEvaluator getFile() {
        return file;
    }

    public ExprEvaluator getClasspathFile() {
        return classpathFile;
    }

    public ExprEvaluator getAppend() {
        return append;
    }

    public EventType getEventType() {
        return eventType;
    }
}
