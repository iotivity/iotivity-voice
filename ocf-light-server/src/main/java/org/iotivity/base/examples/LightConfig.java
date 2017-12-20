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

import org.iotivity.base.OcException;
import org.iotivity.base.OcRepresentation;

/**
 * LightConfig
 *
 * This class represents a light configuration resource
 */
public class LightConfig extends Resource {
    static public final String RES_TYPE = "oic.wk.con";
    static public final String RES_IF = "oic.if.rw";

    static public final String NAME_KEY = "n";
    static public final String NAME_KEY_SIM = "nn"; // using 'nn' so it can be used in the simulator

    private String deviceName;
    private Light lightToConfig; // the light to be configured

    public LightConfig(String name, String uuid, Light light) {
        super("/ocf/light-config/" + uuid, RES_TYPE, RES_IF);
        deviceName = name;
        lightToConfig = light;
    }

    public void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(NAME_KEY_SIM)) {
                deviceName = rep.getValue(NAME_KEY_SIM);
                lightToConfig.setDeviceName(deviceName);
            }
            if (rep.hasAttribute(NAME_KEY)) {
                deviceName = rep.getValue(NAME_KEY);
                lightToConfig.setDeviceName(deviceName);
            }

        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to get representation values");
        }
    }

    public OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();
        try {
            rep.setValue(NAME_KEY, deviceName);
            rep.setValue(NAME_KEY_SIM, deviceName);
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to set representation values");
        }
        return rep;
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String toString() {
        return "[" + super.toString() + ", " + NAME_KEY + ": " + deviceName + "]";
    }
}
