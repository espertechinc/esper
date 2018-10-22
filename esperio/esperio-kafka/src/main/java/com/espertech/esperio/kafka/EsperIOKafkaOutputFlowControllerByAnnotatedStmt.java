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
package com.espertech.esperio.kafka;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.runtime.client.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class EsperIOKafkaOutputFlowControllerByAnnotatedStmt implements EsperIOKafkaOutputFlowController {

    private static final Logger log = LoggerFactory.getLogger(EsperIOKafkaOutputFlowControllerByAnnotatedStmt.class);

    private KafkaProducer producer;
    private EPRuntime runtime;
    private Set<String> topics = new LinkedHashSet<>();

    public void initialize(EsperIOKafkaOutputFlowControllerContext context) {
        this.runtime = context.getRuntime();

        // obtain producer
        try {
            producer = new KafkaProducer<>(context.getProperties());
        } catch (Throwable t) {
            log.error("Error obtaining Kafka producer for URI '{}': {}", context.getRuntime().getURI(), t.getMessage(), t);
        }

        // determine topics
        String topicsCSV = EsperIOKafkaInputAdapter.getRequiredProperty(context.getProperties(), EsperIOKafkaConfig.TOPICS_CONFIG);
        String[] topicNames = topicsCSV.split(",");
        for (String topicName : topicNames) {
            if (topicName.trim().length() > 0) {
                topics.add(topicName.trim());
            }
        }

        // attach to existing statements
        String[] deploymentIds = context.getRuntime().getDeploymentService().getDeployments();
        for (String depoymentId : deploymentIds) {
            EPStatement[] statements = context.getRuntime().getDeploymentService().getDeployment(depoymentId).getStatements();
            for (EPStatement statement : statements) {
                processStatement(statement);
            }
        }

        // attach listener to receive newly-created statements
        runtime.getDeploymentService().addDeploymentStateListener(new DeploymentStateListener() {
            public void onDeployment(DeploymentStateEventDeployed event) {
                for (EPStatement statement : event.getStatements()) {
                    processStatement(statement);
                }
            }

            public void onUndeployment(DeploymentStateEventUndeployed event) {
                for (EPStatement statement : event.getStatements()) {
                    detachStatement(statement);
                }
            }
        });
    }

    private void processStatement(EPStatement statement) {
        if (statement == null) {
            return;
        }
        Annotation annotation = AnnotationUtil.findAnnotation(statement.getAnnotations(), KafkaOutputDefault.class);
        if (annotation == null) {
            return;
        }
        KafkaOutputDefaultListener listener = new KafkaOutputDefaultListener(runtime, statement, producer, topics);
        statement.addListener(listener);
        log.info("Added Kafka-Output-Adapter listener to statement '{}' topics {}", statement.getName(), topics.toString());
    }

    private void detachStatement(EPStatement statement) {
        Iterator<UpdateListener> listeners = statement.getUpdateListeners();
        UpdateListener found = null;
        while (listeners.hasNext()) {
            UpdateListener listener = listeners.next();
            if (listener instanceof KafkaOutputDefaultListener) {
                found = listener;
                break;
            }
        }
        if (found != null) {
            statement.removeListener(found);
        }
        log.info("Removed Kafka-Output-Adapter listener from statement '{}'", statement.getName());
    }

    public void close() {
        producer.close();
    }

    public static class KafkaOutputDefaultListener implements UpdateListener {

        private final JSONEventRenderer jsonEventRenderer;
        private final KafkaProducer producer;
        private final Set<String> topics;

        public KafkaOutputDefaultListener(EPRuntime runtime, EPStatement statement, KafkaProducer producer, Set<String> topics) {
            jsonEventRenderer = runtime.getRenderEventService().getJSONRenderer(statement.getEventType());
            this.producer = producer;
            this.topics = topics;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if (newEvents == null) {
                return;
            }
            for (EventBean event : newEvents) {
                String json = jsonEventRenderer.render(event);
                for (String topic : topics) {
                    producer.send(new ProducerRecord(topic, json));
                }
            }
        }
    }
}
