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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.event.EventBeanManufactureException;
import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esperio.csv.AdapterInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@DataFlowOperator
@DataFlowOpProvideSignal
public class FileSourceLineUnformatted implements DataFlowSourceOperator {

    private static final Logger log = LoggerFactory.getLogger(FileSourceLineUnformatted.class);

    private final AdapterInputSource inputSource;
    private final String propertyNameLine;
    private final String propertyNameFile;

    private Reader reader;
    private InputStream inputStream;
    private BufferedInputStream bis = null;
    private BufferedReader brd = null;

    @DataFlowContext
    protected EPDataFlowEmitter graphContext;

    private LineProcessor lineProcessor;
    private FileBeginEndProcessor bofProcessor;
    private FileBeginEndProcessor eofProcessor;
    private String filenameOrUri;
    private boolean first = true;

    public FileSourceLineUnformatted(AdapterInputSource inputSource, String filenameOrUri, String propertyNameLine, String propertyNameFile) {
        this.inputSource = inputSource;
        this.filenameOrUri = filenameOrUri;
        this.propertyNameLine = propertyNameLine;
        this.propertyNameFile = propertyNameFile;
    }

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        EventType outputEventType = context.getOutputPorts().get(0) != null ? context.getOutputPorts().get(0).getOptionalDeclaredType().getEventType() : null;
        if (outputEventType == null) {
            throw new IllegalArgumentException("No event type provided for output, please provide an event type name");
        }
        if ((outputEventType.getPropertyNames().length != 1 || outputEventType.getPropertyDescriptors()[0].getPropertyType() != String.class) &&
                propertyNameLine == null) {
            throw new IllegalArgumentException("Expecting an output event type that has a single property that is of type string, or alternatively specify the 'propertyNameLine' parameter");
        }

        if (outputEventType instanceof ObjectArrayEventType && outputEventType.getPropertyDescriptors().length == 1) {
            lineProcessor = new LineProcessorObjectArray();
        } else {
            String propertyNameLineToUse = propertyNameLine;
            if (propertyNameLineToUse == null) {
                propertyNameLineToUse = outputEventType.getPropertyDescriptors()[0].getPropertyName();
            }
            if (!outputEventType.isProperty(propertyNameLineToUse)) {
                throw new EPException("Failed to find property name '" + propertyNameLineToUse + "' in type '" + outputEventType.getName() + "'");
            }

            Class propertyType;
            try {
                propertyType = outputEventType.getPropertyType(propertyNameLineToUse);
            } catch (PropertyAccessException ex) {
                throw new EPException("Invalid property name '" + propertyNameLineToUse + "': " + ex.getMessage(), ex);
            }
            if (propertyType != String.class) {
                throw new EPException("Invalid property type for property '" + propertyNameLineToUse + "', expected a property of type String");
            }

            Set<WriteablePropertyDescriptor> writeables = context.getStatementContext().getEventAdapterService().getWriteableProperties(outputEventType, false);
            List<WriteablePropertyDescriptor> writeableList = new ArrayList<WriteablePropertyDescriptor>();

            WriteablePropertyDescriptor writeableLine = EventTypeUtility.findWritable(propertyNameLineToUse, writeables);
            if (writeableLine == null) {
                throw new EPException("Failed to find writable property property '" + propertyNameLineToUse + "', is the property read-only?");
            }
            writeableList.add(writeableLine);

            if (propertyNameFile != null) {
                WriteablePropertyDescriptor writeableFile = EventTypeUtility.findWritable(propertyNameFile, writeables);
                if (writeableFile == null || writeableFile.getType() != String.class) {
                    throw new EPException("Failed to find writable String-type property '" + propertyNameFile + "', is the property read-only?");
                }
                writeableList.add(writeableFile);
            }

            EventBeanManufacturer manufacturer;
            try {
                WriteablePropertyDescriptor[] writables = writeableList.toArray(new WriteablePropertyDescriptor[writeableList.size()]);
                manufacturer = context.getStatementContext().getEventAdapterService().getManufacturer(outputEventType, writables, context.getStatementContext().getEngineImportService(), false);
            } catch (EventBeanManufactureException e) {
                throw new EPException("Event type '" + outputEventType.getName() + "' cannot be written to: " + e.getMessage(), e);
            }

            lineProcessor = new LineProcessorGeneralPurpose(manufacturer);
        }

        if (context.getOutputPorts().size() == 2) {
            eofProcessor = getBeginEndProcessor(context, 1);
        } else if (context.getOutputPorts().size() == 3) {
            bofProcessor = getBeginEndProcessor(context, 1);
            eofProcessor = getBeginEndProcessor(context, 2);
        } else if (context.getOutputPorts().size() > 3) {
            throw new EPException("Operator only allows up to 3 output ports");
        }

        return null;
    }

    public void open(DataFlowOpOpenContext openContext) {
        if (inputSource.getAsReader() != null) {
            reader = inputSource.getAsReader();
            brd = new BufferedReader(reader);
        } else {
            inputStream = inputSource.getAsStream();
            bis = new BufferedInputStream(inputStream);
            brd = new BufferedReader(new InputStreamReader(bis));
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        try {
            if (bis != null) {
                bis.close();
            }
            brd.close();
            if (reader != null) {
                reader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (inputSource != null) {
                inputSource.close();
            }
        } catch (IOException ex) {
            log.error("Failed to close file: " + ex.getMessage(), ex);
        }
    }

    public void next() {
        if (first) {
            first = false;
            if (bofProcessor != null) {
                graphContext.submitPort(1, bofProcessor.processXOF(filenameOrUri));
            }
        }
        String line;
        try {
            line = brd.readLine();
        } catch (IOException e) {
            throw new EPException("Failed to read line: " + e.getMessage(), e);
        }

        if (line != null) {
            if (log.isDebugEnabled()) {
                log.debug("Submitting line '" + line + "'");
            }
            if (eofProcessor != null) {
                graphContext.submitPort(0, lineProcessor.process(line, filenameOrUri));
            } else {
                graphContext.submit(lineProcessor.process(line, filenameOrUri));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Submitting punctuation");
            }
            if (eofProcessor != null) {
                int port = bofProcessor != null ? 2 : 1;
                graphContext.submitPort(port, eofProcessor.processXOF(filenameOrUri));
            }
            graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
            });
        }
    }

    private FileBeginEndProcessor getBeginEndProcessor(DataFlowOpInitializateContext context, int outputPort) {
        EventType portEventType = context.getOutputPorts().get(outputPort).getOptionalDeclaredType().getEventType();
        Set<WriteablePropertyDescriptor> writeables = context.getStatementContext().getEventAdapterService().getWriteableProperties(portEventType, false);
        List<WriteablePropertyDescriptor> writeableList = new ArrayList<WriteablePropertyDescriptor>();
        EventBeanManufacturer manufacturer;
        if (propertyNameFile != null) {
            WriteablePropertyDescriptor writeableFile = EventTypeUtility.findWritable(propertyNameFile, writeables);
            if (writeableFile == null || writeableFile.getType() != String.class) {
                throw new EPException("Failed to find writable String-type property '" + propertyNameFile + "', is the property read-only?");
            }
            writeableList.add(writeableFile);
        }
        try {
            manufacturer = context.getStatementContext().getEventAdapterService().getManufacturer(portEventType, writeableList.toArray(new WriteablePropertyDescriptor[writeableList.size()]), context.getStatementContext().getEngineImportService(), false);
        } catch (EventBeanManufactureException e) {
            throw new EPException("Event type '" + portEventType.getName() + "' cannot be written to: " + e.getMessage(), e);
        }
        return new FileBeginEndProcessorGeneralPurpose(manufacturer);
    }

    private static interface LineProcessor {
        public Object process(String line, String filename);
    }

    private static class LineProcessorObjectArray implements LineProcessor {
        public Object process(String line, String filename) {
            return new Object[]{line};
        }
    }

    private static class LineProcessorGeneralPurpose implements LineProcessor {
        private final EventBeanManufacturer manufacturer;

        public LineProcessorGeneralPurpose(EventBeanManufacturer manufacturer) {
            this.manufacturer = manufacturer;
        }

        public Object process(String line, String filename) {
            return manufacturer.makeUnderlying(new Object[]{line, filename});
        }
    }

    private static interface FileBeginEndProcessor {
        public Object processXOF(String filename);
    }

    private static class FileBeginEndProcessorGeneralPurpose implements FileBeginEndProcessor {
        private final EventBeanManufacturer manufacturer;

        public FileBeginEndProcessorGeneralPurpose(EventBeanManufacturer manufacturer) {
            this.manufacturer = manufacturer;
        }

        public Object processXOF(String filename) {
            return manufacturer.makeUnderlying(new Object[]{filename});
        }
    }
}
