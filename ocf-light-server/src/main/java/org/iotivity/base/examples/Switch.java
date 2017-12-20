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

import org.iotivity.base.ErrorCode;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;

/**
 * Switch
 *
 * This class represents a binary switch resource
 */
public class Switch extends Resource implements LightImageObserver {
    static public final String RES_TYPE = "oic.r.switch.binary";
    static public final String RES_IF = "oic.if.a";

    static public final String VALUE_KEY = "value";

    private boolean powerOn;

    public Switch(String uuid) {
        super("/ocf/switch/" + uuid, RES_TYPE, RES_IF);
    }

    public void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(VALUE_KEY)) {
                powerOn = rep.getValue(VALUE_KEY);
            }
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to get representation values");
        }
    }

    public OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();
        try {
            rep.setValue(VALUE_KEY, powerOn);
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to set representation values");
        }
        return rep;
    }

    public boolean getValue() {
        return powerOn;
    }

    public void setValue(boolean value) {
        powerOn = value;
    }

    @Override
    public void update(boolean powerOn, int brightness) {
        setValue(powerOn);
        try {
            OcPlatform.notifyAllObservers(getResourceHandle());
        } catch (OcException e) {
            ErrorCode errorCode = e.getErrorCode();
            if (ErrorCode.NO_OBSERVERS == errorCode) {
//                OcfLightDevice.msg("No observers found");
            } else {
                OcfLightDevice.msgError(e.toString());
                OcfLightDevice.msgError("Failed to notify observers");
            }
        }
    }

    @Override
    public String toString() {
        return "[" + super.toString() + ", " + VALUE_KEY + ": " + powerOn + "]";
    }
}
