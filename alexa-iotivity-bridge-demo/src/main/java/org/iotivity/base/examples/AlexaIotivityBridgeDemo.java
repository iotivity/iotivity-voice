/*
 *******************************************************************
 *
 * Copyright 2017 Intel Corporation.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

package org.iotivity.base.examples;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;

import com.amazonaws.services.iot.client.sample.sampleUtil.CommandArguments;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;

/**
 * AlexaIotivityBridgeDemo
 *
 * AlexaIotivityBridgeDemo is a client app which finds resources
 * advertised by the server pushes them to the cloud and waits
 * for update requests.
 */
public class AlexaIotivityBridgeDemo {

    private static final String UpdateTopic = "$aws/things/larryslinuxbox/shadow/update";
    private static final String UpdateAcceptedTopic = "$aws/things/larryslinuxbox/shadow/update/accepted";
    private static final AWSIotQos TopicQos = AWSIotQos.QOS0;

    private static AWSIotMqttClient awsIotClient;
    private static IotivityClient iotivityClient;

    // initClient() is copied from aws iot sdk samples.
    private static void initAwsIotClient(CommandArguments arguments) {
        String clientEndpoint = arguments.getNotNull("clientEndpoint", SampleUtil.getConfig("clientEndpoint"));
        String clientId = arguments.getNotNull("clientId", SampleUtil.getConfig("clientId"));

        String certificateFile = arguments.get("certificateFile", SampleUtil.getConfig("certificateFile"));
        String privateKeyFile = arguments.get("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));
        if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
            String algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));
            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

            awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }

        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }

    /**
     * Configure and initialize platform.
     */
    private static void startIotivityClient() {

        PlatformConfig platformConfig = new PlatformConfig(
                ServiceType.IN_PROC,
                ModeType.CLIENT,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );
        msg("Configuring platform.");
        OcPlatform.Configure(platformConfig);

        iotivityClient = new IotivityClient();
    }

    public static void main(String args[]) throws IOException, AWSIotException, AWSIotTimeoutException, InterruptedException {
        CommandArguments arguments = CommandArguments.parse(args);
        initAwsIotClient(arguments); // creates awsIotClient

        startIotivityClient(); // creates iotivityClient

        awsIotClient.setWillMessage(new AWSIotMessage("client/disconnect", AWSIotQos.QOS0, awsIotClient.getClientId()));

        String thingName = arguments.getNotNull("thingName", SampleUtil.getConfig("thingName"));
        ConnectedThing connectedThing = new ConnectedThing(thingName, iotivityClient);

        awsIotClient.attach(connectedThing);
        awsIotClient.connect();

        // Delete existing document if any
        connectedThing.delete();

        AWSIotTopic topic = new UpdateAcceptedTopicListener(UpdateAcceptedTopic, TopicQos);
        awsIotClient.subscribe(topic, false);

        iotivityClient.setConnectedThing(connectedThing);

        AWSIotConnectionStatus status = AWSIotConnectionStatus.DISCONNECTED;

        while (true) {
            AWSIotConnectionStatus newStatus = awsIotClient.getConnectionStatus();
            if (!status.equals(newStatus)) {
                AlexaIotivityBridgeDemo.msg("Connection status changed to " + newStatus);
                status = newStatus;
            }

            try {
//                msg("Finding all resources of type " + Light.OIC_TYPE_DEVICE_LIGHT);
                String requestUri = OcPlatform.WELL_KNOWN_QUERY + "?rt=" + Light.OIC_TYPE_DEVICE_LIGHT;
                OcPlatform.findResources("", requestUri, EnumSet.of(OcConnectivityType.CT_DEFAULT), iotivityClient);

            } catch (OcException e) {
                msgError(e.toString());
                msgError("Failed to invoke find resource API");
            }

            sleep(10);

            iotivityClient.cancelObserve();
        }
    }

    public static void publishUpdatePayload(final String payload) {
        AWSIotMessage message = new UpdatePublisherListener(UpdateTopic, TopicQos, payload);

        try {
            awsIotClient.publish(message);
        } catch (AWSIotException e) {
            AlexaIotivityBridgeDemo.msgError("publish failed for " + payload);
        }
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            msgError(e.toString());
        }
    }

    public static void msg(final String text) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println(dateFormat.format(date) + " " + text);
    }

    public static void msgError(final String text) {
        msg("[Error] " + text);
    }

    class ResourceNameComparator implements Comparator<Resource> {
        @Override
        public int compare(Resource lhs, Resource rhs) {
            return (lhs.getName().compareToIgnoreCase(rhs.getName()));
        }
    }
}
