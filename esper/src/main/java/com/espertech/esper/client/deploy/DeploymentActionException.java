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
package com.espertech.esper.client.deploy;

import java.io.StringWriter;
import java.util.List;

/**
 * Exception list populated in a deployment operation.
 */
public class DeploymentActionException extends DeploymentException {

    private static final long serialVersionUID = -2738808350555092087L;

    private static String newline = System.getProperty("line.separator");

    private List<DeploymentItemException> exceptions;

    /**
     * Ctor.
     *
     * @param message    deployment error message
     * @param exceptions that occured deploying
     */
    public DeploymentActionException(String message, List<DeploymentItemException> exceptions) {
        super(message, exceptions.isEmpty() ? null : exceptions.get(0));
        this.exceptions = exceptions;
    }

    /**
     * Returns the exception list.
     *
     * @return exceptions
     */
    public List<DeploymentItemException> getExceptions() {
        return exceptions;
    }

    /**
     * Returns a detail print of all exceptions and messages line-separated.
     *
     * @return exception list
     */
    public String getDetail() {
        StringWriter detail = new StringWriter();
        int count = 0;
        String delimiter = "";
        for (DeploymentItemException item : exceptions) {
            detail.write(delimiter);
            detail.write("Exception #");
            detail.write(Integer.toString(count));
            detail.write(" : ");
            detail.write(item.getInner().getMessage());
            delimiter = newline + newline;
            count++;
        }
        return detail.toString();
    }
}
