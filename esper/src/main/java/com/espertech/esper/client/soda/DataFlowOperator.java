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
import java.io.StringWriter;
import java.util.List;

/**
 * Object model of a data flow operator declaration.
 */
public class DataFlowOperator implements Serializable {

    private static final long serialVersionUID = 3702665008312151743L;
    private List<AnnotationPart> annotations;
    private String operatorName;
    private List<DataFlowOperatorInput> input;
    private List<DataFlowOperatorOutput> output;
    private List<DataFlowOperatorParameter> parameters;

    /**
     * Ctor
     *
     * @param annotations  annotations
     * @param operatorName operator name
     * @param input        input stream definitions
     * @param output       output stream definitions
     * @param parameters   parameters
     */
    public DataFlowOperator(List<AnnotationPart> annotations, String operatorName, List<DataFlowOperatorInput> input, List<DataFlowOperatorOutput> output, List<DataFlowOperatorParameter> parameters) {
        this.annotations = annotations;
        this.operatorName = operatorName;
        this.input = input;
        this.output = output;
        this.parameters = parameters;
    }

    /**
     * Ctor.
     */
    public DataFlowOperator() {
    }

    /**
     * Returns the annotations.
     *
     * @return annotations
     */
    public List<AnnotationPart> getAnnotations() {
        return annotations;
    }

    /**
     * Sets the annotations.
     *
     * @param annotations to set
     */
    public void setAnnotations(List<AnnotationPart> annotations) {
        this.annotations = annotations;
    }

    /**
     * Returns the operator name.
     *
     * @return operator name
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Sets the operator name.
     *
     * @param operatorName operator name
     */
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    /**
     * Returns the input stream definitions, if any.
     *
     * @return input streams
     */
    public List<DataFlowOperatorInput> getInput() {
        return input;
    }

    /**
     * Sets the input stream definitions, if any.
     *
     * @param input input streams
     */
    public void setInput(List<DataFlowOperatorInput> input) {
        this.input = input;
    }

    /**
     * Returns the output stream definitions, if any.
     *
     * @return output streams
     */
    public List<DataFlowOperatorOutput> getOutput() {
        return output;
    }

    /**
     * Sets the output stream definitions, if any.
     *
     * @param output streams
     */
    public void setOutput(List<DataFlowOperatorOutput> output) {
        this.output = output;
    }

    /**
     * Returns operator parameters.
     * <p>
     * Object values may be expressions, constants, JSON values or EPL statements.
     * </p>
     *
     * @return map of parameters
     */
    public List<DataFlowOperatorParameter> getParameters() {
        return parameters;
    }

    /**
     * Sets operator parameters.
     * <p>
     * Object values may be expressions, constants, JSON values or EPL statements.
     * </p>
     *
     * @param parameters map of parameters
     */
    public void setParameters(List<DataFlowOperatorParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Render to string.
     *
     * @param writer    to render
     * @param formatter for formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write(operatorName);

        if (!input.isEmpty()) {
            writer.write("(");
            String delimiter = "";
            for (DataFlowOperatorInput inputItem : input) {
                writer.write(delimiter);
                writeInput(inputItem, writer);
                if (inputItem.getOptionalAsName() != null) {
                    writer.write(" as ");
                    writer.write(inputItem.getOptionalAsName());
                }
                delimiter = ", ";
            }
            writer.write(")");
        }

        if (!output.isEmpty()) {
            writer.write(" -> ");
            String delimiter = "";
            for (DataFlowOperatorOutput outputItem : output) {
                writer.write(delimiter);
                writer.write(outputItem.getStreamName());
                writeTypes(outputItem.getTypeInfo(), writer);
                delimiter = ", ";
            }
        }

        if (parameters.isEmpty()) {
            writer.write(" {}");
            formatter.endDataFlowOperatorDetails(writer);
        } else {
            writer.write(" {");
            formatter.beginDataFlowOperatorDetails(writer);
            String delimiter = ",";
            int count = 0;
            for (DataFlowOperatorParameter parameter : parameters) {
                parameter.toEpl(writer);
                count++;
                if (parameters.size() > count) {
                    writer.write(delimiter);
                }
                formatter.endDataFlowOperatorConfig(writer);
            }
            writer.write("}");
            formatter.endDataFlowOperatorDetails(writer);
        }
    }

    private void writeInput(DataFlowOperatorInput inputItem, StringWriter writer) {
        if (inputItem.getInputStreamNames().size() > 1) {
            String delimiterNames = "";
            writer.write("(");
            for (String name : inputItem.getInputStreamNames()) {
                writer.write(delimiterNames);
                writer.write(name);
                delimiterNames = ", ";
            }
            writer.write(")");
        } else {
            writer.write(inputItem.getInputStreamNames().get(0));
        }
    }

    private void writeTypes(List<DataFlowOperatorOutputType> types, StringWriter writer) {
        if (types == null || types.isEmpty()) {
            return;
        }

        writer.write("<");
        String typeDelimiter = "";
        for (DataFlowOperatorOutputType type : types) {
            writer.write(typeDelimiter);
            writeType(type, writer);
            typeDelimiter = ",";
        }
        writer.write(">");
    }

    private void writeType(DataFlowOperatorOutputType type, StringWriter writer) {
        if (type.isWildcard()) {
            writer.append('?');
            return;
        }
        writer.append(type.getTypeOrClassname());
        writeTypes(type.getTypeParameters(), writer);
    }
}
