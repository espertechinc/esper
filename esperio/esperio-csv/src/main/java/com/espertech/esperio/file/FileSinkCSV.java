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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorLifecycle;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowSignalHandler;
import com.espertech.esper.common.internal.event.core.EventBeanSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.render.GetterPair;
import com.espertech.esper.common.internal.event.render.RendererMeta;
import com.espertech.esper.common.internal.event.render.RendererMetaOptions;
import com.espertech.esper.common.internal.util.FileUtil;

import java.io.*;
import java.util.Stack;

public class FileSinkCSV implements DataFlowOperatorLifecycle, EPDataFlowSignalHandler {
    private final static String NEWLINE = System.getProperty("line.separator");

    private final FileSinkFactory factory;
    private final String filename;
    private final boolean classpathFile;
    private final boolean append;

    private Writer writer;
    private FileOutputStream fos;
    private RendererMeta rendererMeta;
    private RendererMetaOptions rendererOptions;
    private EventBeanSPI eventShell;

    public FileSinkCSV(FileSinkFactory factory, String filename, boolean classpathFile, boolean append) {
        this.factory = factory;
        this.filename = filename;
        this.classpathFile = classpathFile;
        this.append = append;

        rendererOptions = new RendererMetaOptions(true, false, null, null);
        rendererMeta = new RendererMeta(factory.getEventType(), new Stack<>(), rendererOptions);
        eventShell = EventTypeUtility.getShellForType(factory.getEventType());
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
            if (!(eventShell.getEventType() instanceof JsonEventType)) {
                eventShell.setUnderlying(object);
            } else {
                JsonEventType jsonEventType = (JsonEventType) eventShell.getEventType();
                Object underlying = jsonEventType.parse(object.toString());
                eventShell.setUnderlying(underlying);
            }
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
