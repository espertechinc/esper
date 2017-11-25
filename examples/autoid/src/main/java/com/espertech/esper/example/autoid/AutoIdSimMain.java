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
package com.espertech.esper.example.autoid;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Random;

public class AutoIdSimMain implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(AutoIdSimMain.class);

    private final static Random RANDOM = new Random(System.currentTimeMillis());
    private final static String[] SENSOR_IDS = {"urn:epc:1:4.16.30", "urn:epc:1:4.16.32", "urn:epc:1:4.16.36", "urn:epc:1:4.16.38"};
    private final static String XML_ROOT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<pmlcore:Sensor \n" +
            "  xmlns=\"urn:autoid:specification:interchange:PMLCore:xml:schema:1\" \n" +
            "  xmlns:pmlcore=\"urn:autoid:specification:interchange:PMLCore:xml:schema:1\" \n" +
            "  xmlns:autoid=\"http://www.autoidcenter.org/2003/xml\" \n" +
            "  xmlns:pmluid=\"urn:autoid:specification:universal:Identifier:xml:schema:1\" \n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
            "  xsi:schemaLocation=\"urn:autoid:specification:interchange:PMLCore:xml:schema:1 AutoIdPmlCore.xsd\">\n";

    private final int numEvents;
    private final String engineURI;
    private final boolean continuousSimulation;
    private final DocumentBuilder documentBuilder;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        if (args.length < 1) {
            System.out.println("Arguments are: <numberOfEvents>");
            System.exit(-1);
        }

        int events;
        try {
            events = Integer.parseInt(args[0]);
        } catch (NullPointerException e) {
            System.out.println("Invalid numberOfEvents: " + args[0]);
            System.exit(-2);
            return;
        }

        if (events > 1000) {
            System.out.println("Invalid numberOfEvents: " + args[0]);
            System.out.println("The maxiumum for this example is 1000 events, since the example retains the last 60 seconds of events and each event is an XML document, and heap memory size is 256k for this example.");
            System.exit(-2);
            return;
        }

        // Run the sample
        AutoIdSimMain autoIdSimMain = new AutoIdSimMain(events, "AutoIDSim", false);
        autoIdSimMain.run();
    }

    public AutoIdSimMain(int numEvents, String engineURI, boolean continuousSimulation) throws ParserConfigurationException {
        this.numEvents = numEvents;
        this.engineURI = engineURI;
        this.continuousSimulation = continuousSimulation;

        // set up DOM parser
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        documentBuilder = builderFactory.newDocumentBuilder();
    }

    public void run() {
        // load config - this defines the XML event types to be processed
        String configFile = "esper.examples.cfg.xml";
        URL url = AutoIdSimMain.class.getClassLoader().getResource(configFile);
        if (url == null) {
            log.error("Error loading configuration file '" + configFile + "' from classpath");
            return;
        }
        Configuration config = new Configuration();
        config.configure(url);

        // get engine instance
        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, config);

        // set up statement
        RFIDTagsPerSensorStmt rfidStmt = new RFIDTagsPerSensorStmt(epService.getEPAdministrator());
        rfidStmt.addListener(new RFIDTagsPerSensorListener());

        // Send events
        if (!continuousSimulation) {
            int eventCount = 0;
            while (eventCount < numEvents) {
                sendEvent(epService.getEPRuntime());
                eventCount++;
            }
        } else {
            while (true) {
                sendEvent(epService.getEPRuntime());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        epService.destroy();
    }

    private void sendEvent(EPRuntime epRuntime) {
        try {
            String eventXMLText = generateEvent();
            Document simpleDoc = documentBuilder.parse(new InputSource(new StringReader(eventXMLText)));
            epRuntime.sendEvent(simpleDoc);
        } catch (Exception ex) {
            throw new RuntimeException("Error sending event: " + ex.getMessage(), ex);
        }
    }

    private String generateEvent() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(XML_ROOT);

        String sensorId = SENSOR_IDS[RANDOM.nextInt(SENSOR_IDS.length)];
        buffer.append("<pmluid:ID>");
        buffer.append(sensorId);
        buffer.append("</pmluid:ID>");

        buffer.append("<pmlcore:Observation>");
        buffer.append("<pmlcore:Command>READ_PALLET_TAGS_ONLY</pmlcore:Command>");

        for (int i = 0; i < RANDOM.nextInt(6) + 1; i++) {
            buffer.append("<pmlcore:Tag><pmluid:ID>urn:epc:1:2.24.400</pmluid:ID></pmlcore:Tag>");
        }

        buffer.append("</pmlcore:Observation>");
        buffer.append("</pmlcore:Sensor>");

        return buffer.toString();
    }

    public void destroy() {
        EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
    }
}
