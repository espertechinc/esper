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

public class GraphOperatorSpec implements Serializable {
    private static final long serialVersionUID = 7606589198404851791L;
    private final String operatorName;
    private final GraphOperatorInput input;
    private final GraphOperatorOutput output;
    private final GraphOperatorDetail detail;
    private final List<AnnotationDesc> annotations;

    public GraphOperatorSpec(String operatorName, GraphOperatorInput input, GraphOperatorOutput output, GraphOperatorDetail detail, List<AnnotationDesc> annotations) {
        this.operatorName = operatorName;
        this.input = input;
        this.output = output;
        this.detail = detail;
        this.annotations = annotations;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public GraphOperatorInput getInput() {
        return input;
    }

    public GraphOperatorOutput getOutput() {
        return output;
    }

    public GraphOperatorDetail getDetail() {
        return detail;
    }

    public List<AnnotationDesc> getAnnotations() {
        return annotations;
    }
}
