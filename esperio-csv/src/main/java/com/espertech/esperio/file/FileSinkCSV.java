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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.event.EventBeanSPI;
import com.espertech.esper.event.util.EventTypePropertyPair;
import com.espertech.esper.event.util.GetterPair;
import com.espertech.esper.event.util.RendererMeta;
import com.espertech.esper.event.util.RendererMetaOptions;
import com.espertech.esper.util.FileUtil;

import java.io.*;
import java.util.Stack;

public class FileSinkCSV implements DataFlowOpLifecycle, EPDataFlowSignalHandler {
    private final static String NEWLINE = System.getProperty("line.separator");

    private final String filename;
    private final boolean classpathFile;
    private final boolean append;

    public FileSinkCSV(String filename, boolean classpathFile, boolean append) {
        this.filename = filename;
        this.classpathFile = classpathFile;
        this.append = append;
    }

    private Writer writer;
    private FileOutputStream fos;
    private RendererMeta rendererMeta;
    private RendererMetaOptions rendererOptions;
    private EventBeanSPI eventShell;

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        if (context.getInputPorts().size() != 1) {
            throw new EPException(this.getClass().getSimpleName() + " expected a single input port");
        }
        EventType eventType = context.getInputPorts().get(0).getTypeDesc().getEventType();
        if (eventType == null) {
            throw new EPException("No event type defined for input port");
        }

        rendererOptions = new RendererMetaOptions(true, false, null, null);
        rendererMeta = new RendererMeta(eventType, new Stack<EventTypePropertyPair>(), rendererOptions);
        eventShell = context.getServicesContext().getEventAdapterService().getShellForType(eventType);

        return null;
    }

    public void open(DataFlowOpOpenContext openContext) {
        File file;
        if (classpathFile) {
            String filenameCPDir = filename;
            String filenameAlone = filename;
            int index = filename.lastIndexOf(File.separatorChar);
            if (index != -1) {
                filenameCPDir = filename.substring(0, index);
                filenameAlone = filename.substring(index + 1);
            } else {
                index = filename.lastIndexOf('\\');
                if (index != -1) {
                    filenameCPDir = filename.substring(0, index);
                    filenameAlone = filename.substring(index + 1);
                } else {
                    index = filename.lastIndexOf('/');
                    if (index != -1) {
                        filenameCPDir = filename.substring(0, index);
                        filenameAlone = filename.substring(index + 1);
                    }
                }
            }
            String fileCP = FileUtil.findClasspathFile(filenameCPDir);
            if (fileCP == null) {
                throw new EPException("Failed to find path '" + filenameCPDir + "' in classpath");
            }
            file = new File(fileCP);
            if (!file.isDirectory()) {
                throw new EPException("File '" + filenameCPDir + "' is not a directory");
            }
            if (filenameAlone.length() == 0) {
                throw new EPException("Failed to find filename in 'path/filename' format for '" + filename + "'");
            }
            file = new File(fileCP + File.separatorChar + filenameAlone);
        } else {
            file = new File(filename);
        }

        if (file.exists() && !append) {
            throw new EPException("File already exists '" + filename + "'");
        }

        try {
            fos = new FileOutputStream(file, append);
            writer = new OutputStreamWriter(fos);
        } catch (FileNotFoundException e) {
            throw new EPException("Failed to open '" + file.getAbsolutePath() + "' for writing");
        }
    }

    public void onInput(Object object) {
        try {
            StringBuilder buf = new StringBuilder();
            eventShell.setUnderlying(object);
            recursiveRender(eventShell, buf, 0, rendererMeta, rendererOptions);
            writer.write(buf.toString());
            writer.flush();
            fos.flush();
            fos.getChannel().force(true);
            fos.getFD().sync();
        } catch (IOException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                }
                writer = null;
            }
        }
    }

    public void onSignal(EPDataFlowSignal signal) {
        if (signal instanceof EPDataFlowSignalFinalMarker) {
            destroy();
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        destroy();
    }

    private static void recursiveRender(EventBean theEvent, StringBuilder buf, int level, RendererMeta meta, RendererMetaOptions rendererOptions) {
        String delimiter = "";
        GetterPair[] simpleProps = meta.getSimpleProperties();
        for (GetterPair simpleProp : simpleProps) {
            Object value = simpleProp.getGetter().get(theEvent);
            buf.append(delimiter);
            simpleProp.getOutput().render(value, buf);
            delimiter = ",";
        }
        buf.append(NEWLINE);
    }

    private void destroy() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e1) {
            }
            writer = null;
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e1) {
            }
            fos = null;
        }
    }
}
