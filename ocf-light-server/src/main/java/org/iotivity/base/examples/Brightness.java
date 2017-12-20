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
 * Brightness
 *
 * This class represents a rightness resource
 */
public class Brightness extends Resource implements LightImageObserver {
    static public final String RES_TYPE = "oic.r.light.brightness";
    static public final String RES_IF = "oic.if.a";

    static public final String BRIGHTNESS_KEY = "brightness";

    private int brightness;

    public Brightness(String uuid) {
        super("/ocf/brightness/" + uuid, RES_TYPE, RES_IF);
    }

    public void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(BRIGHTNESS_KEY)) {
                brightness = rep.getValue(BRIGHTNESS_KEY);
                brightness = Math.max(0, brightness);
                brightness = Math.min(100, brightness);
            }
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to get representation values");
        }
    }

    public OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();
        try {
            brightness = Math.max(0, brightness);
            brightness = Math.min(100, brightness);
            rep.setValue(BRIGHTNESS_KEY, brightness);
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to set representation values");
        }
        return rep;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        brightness = Math.max(0, brightness);
        brightness = Math.min(100, brightness);
        this.brightness = brightness;
    }

    @Override
    public void update(boolean powerOn, int brightness) {
        setBrightness(brightness);
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
        return "[" + super.toString() + ", " + BRIGHTNESS_KEY + ": " + brightness + "]";
    }
}
