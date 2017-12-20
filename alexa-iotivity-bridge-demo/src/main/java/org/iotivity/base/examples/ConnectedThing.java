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

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotDeviceProperty;

/**
 * This class encapsulates an actual device. It extends {@link AWSIotDevice} to
 * define properties that are to be kept in sync with the AWS IoT shadow.
 */
public class ConnectedThing extends AWSIotDevice {

    static private IotivityClient iotivityClient;

    @AWSIotDeviceProperty
    private LightDevice[] lightDevices = new LightDevice[0];

//    @AWSIotDeviceProperty
    private int lightState; // global light state

    public ConnectedThing(String thingName, IotivityClient iotivityClient) {
        super(thingName);
        ConnectedThing.iotivityClient = iotivityClient;
    }

    public int getLightState() {
        // 1. read the light state from the light actuator
        int reportedState = lightState;
//        AlexaIotivityBridgeDemo.msg(">>> reported lightState: " + (reportedState != 0 ? "on" : "off"));

        // 2. return the current light state
        return reportedState;
    }

    public void setLightState(int desiredState) {
        // 1. update the light actuator with the desired state
        lightState = desiredState;
        AlexaIotivityBridgeDemo.msg("<<< desired lightState to " + (desiredState != 0 ? "on" : "off"));

        // 2. tell the iotivity client to update iotivity resources
        for (LightDevice lightDevice : lightDevices) {
            lightDevice.setPowerOn(desiredState != 0);
            iotivityClient.updateLight(lightDevice.getName(), desiredState != 0, lightDevice.getBrightness());
        }
    }

    public LightDevice[] getLightDevices() {
        LightDevice[] reportedLightDevices = lightDevices;
//        AlexaIotivityBridgeDemo.msg(">>> reported light devices length = " + reportedLightDevices.length);
        return reportedLightDevices;
    }

    public void setLightDevices(LightDevice[] desiredLightDevices) {
        if (desiredLightDevices != null) {
            lightDevices = desiredLightDevices;
//            AlexaIotivityBridgeDemo.msg(">>> desired light devices length = " + desiredLightDevices.length);
        }
    }

    static public class LightDevice {

        @AWSIotDeviceProperty
        private String uri = "";

        @AWSIotDeviceProperty
        private String name = "";

        @AWSIotDeviceProperty
        private boolean powerOn;

        @AWSIotDeviceProperty
        private int brightness = -1; // uninitialized

        public String getUri() {
            String reportedUri = uri;
//            AlexaIotivityBridgeDemo.msg(">>> reported uri: " + reportedUri);
            return reportedUri;
        }

        public void setUri(String desiredUri) {
            uri = desiredUri;
            AlexaIotivityBridgeDemo.msg("<<< desired uri: " + desiredUri);
        }

        public String getName() {
            String reportedName = name;

            // fake for changed upnp device name
            if (NamesPropertyFile.getInstance().hasUri(uri)) {
                reportedName = NamesPropertyFile.getInstance().getNameForUri(uri);
            }

//            AlexaIotivityBridgeDemo.msg(">>> reported name: " + reportedName);
            return reportedName;
        }

        public void setName(String desiredName) {
            if (!name.equals(desiredName)) {
                name = desiredName;
                AlexaIotivityBridgeDemo.msg("<<< desired name: " + desiredName);

                // update names property file
                NamesPropertyFile.getInstance().updateNamesProperty(uri, name);
                iotivityClient.updateLight(uri, name);
            }
        }

        public boolean getPowerOn() {
            boolean reportedPowerOn = powerOn;
//            AlexaIotivityBridgeDemo.msg(">>> reported powerOn: " + reportedPowerOn);
            return reportedPowerOn;
        }

        public void setPowerOn(boolean desiredPowerOn) {
            if (powerOn != desiredPowerOn) {
                powerOn = desiredPowerOn;

                AlexaIotivityBridgeDemo.msg("<<< desired powerOn: " + desiredPowerOn);
                
                if (brightness >= 0) {
                    iotivityClient.updateLight(uri, powerOn, brightness);
                }
            }
        }

        public int getBrightness() {
            int reportedBrightness = brightness;
//            AlexaIotivityBridgeDemo.msg(">>> reported brightness: " + reportedBrightness);
            return reportedBrightness;
        }

        public void setBrightness(int desiredBrightness) {
            if (brightness != desiredBrightness) {
                brightness = desiredBrightness;

                AlexaIotivityBridgeDemo.msg("<<< desired brightness: " + brightness);

                iotivityClient.updateLight(uri, powerOn, brightness);
            }
        }
    }
}
