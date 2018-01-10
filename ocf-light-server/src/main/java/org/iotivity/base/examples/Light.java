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
import org.iotivity.base.PayloadType;

/**
 * Light
 *
 * This class represents a light resource
 */
public class Light extends Resource implements LightImageObserver {
    static public final String RES_TYPE = "oic.d.light";
    static public final String DEVICE_RES_TYPE = "oic.wk.d";

    static private final String COL_RES_TYPE = "oic.wk.col";

    static private final String VENDOR_RES_TYPE = "x.org.examples.light";

    static private final String RTS_KEY = "rts";
    static private final String LINKS_KEY = "links";

    private Links resourceLinks;
    private String[] collectionResTypes = {Switch.RES_TYPE, Brightness.RES_TYPE};

    private LightConfig lightConfigRes;
    private Switch switchRes;
    private Brightness brightnessRes;

    private String deviceName;
    private boolean powerOn;
    private int brightness;

    public Light(boolean useLinks, String name, String uuid, boolean powerOn, int brightness, LightPanel ui) {
        super("/ocf/light/" + uuid, (useLinks ? RES_TYPE : VENDOR_RES_TYPE), OcPlatform.DEFAULT_INTERFACE);

        if (useLinks) {
            lightConfigRes = new LightConfig(name, uuid, this);
            lightConfigRes.addObserver(ui);
            OcfLightDevice.msg("Created config resource: " + lightConfigRes);

            switchRes = new Switch(uuid);
            switchRes.setValue(powerOn);
            switchRes.addObserver(ui);
            ui.addObserver(switchRes);
            OcfLightDevice.msg("Created switch resource: " + switchRes);

            brightnessRes = new Brightness(uuid);
            brightnessRes.setBrightness(brightness);
            brightnessRes.addObserver(ui);
            ui.addObserver(brightnessRes);
            OcfLightDevice.msg("Created brightness resource: " + brightnessRes);

        } else {
            deviceName = name;
            this.powerOn = powerOn;
            this.brightness = brightness;
            addObserver(ui);
            ui.addObserver(this);
        }

        try {
            OcPlatform.setPropertyValue(PayloadType.PLATFORM.getValue(), "mnmn", "Intel");

            setDeviceName(name);
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(), "icv", "ocf.1.0.0");
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(), "dmv", "ocf.res.1.0.0,ocf.sh.1.0.0");

        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to create properties for " + getResourceUri());
            e.printStackTrace();
        }

        if (useLinks) {
            Link[] links = new Link[4];
            links[0] = new Link(lightConfigRes.getResourceUri(), new String[] { LightConfig.RES_TYPE });
            links[1] = new Link(switchRes.getResourceUri(), new String[] { Switch.RES_TYPE });
            links[2] = new Link(brightnessRes.getResourceUri(), new String[] { Brightness.RES_TYPE });
            links[3] = new Link("/oic/d", new String[] { DEVICE_RES_TYPE, RES_TYPE });
            resourceLinks = new Links(links);

            try {
                OcPlatform.bindInterfaceToResource(getResourceHandle(), OcPlatform.LINK_INTERFACE);
                OcPlatform.bindTypeToResource(getResourceHandle(), COL_RES_TYPE);

            } catch (OcException e) {
                OcfLightDevice.msgError("Failed to bind link interface for " + getResourceUri());
                e.printStackTrace();
            }

        } else {
            try {
                OcPlatform.bindInterfaceToResource(getResourceHandle(), LightConfig.RES_IF);
                OcPlatform.bindTypeToResource(getResourceHandle(), LightConfig.RES_TYPE);

                OcPlatform.bindInterfaceToResource(getResourceHandle(), Switch.RES_IF);
                OcPlatform.bindTypeToResource(getResourceHandle(), Switch.RES_TYPE);

                OcPlatform.bindInterfaceToResource(getResourceHandle(), Brightness.RES_IF);
                OcPlatform.bindTypeToResource(getResourceHandle(), Brightness.RES_TYPE);

            } catch (OcException e) {
                OcfLightDevice.msgError("Failed to bind interface or type for " + getResourceUri());
                e.printStackTrace();
            }
        }

        OcfLightDevice.msg("Created light resource: " + this);
    }

    public void unregister() {
        try {
            lightConfigRes.unregisterResource();
        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to unregister " + lightConfigRes.getResourceUri());
            e.printStackTrace();
        }
        try {
            switchRes.unregisterResource();
        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to unregister " + switchRes.getResourceUri());
            e.printStackTrace();
        }
        try {
            brightnessRes.unregisterResource();
        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to unregister " + brightnessRes.getResourceUri());
            e.printStackTrace();
        }
        try {
            unregisterResource();
        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to unregister " + getResourceUri());
            e.printStackTrace();
        }
    }

    public void setDeviceName(String name) {
        try {
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(), "n", name);

        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to set device name to " + name);
            e.printStackTrace();
        }
    }

    public void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(LINKS_KEY)) {
                OcRepresentation[] links = rep.getValue(LINKS_KEY);
                resourceLinks.setOcRepresentation(links);
            }
            if (rep.hasAttribute(LightConfig.NAME_KEY_SIM)) {
                deviceName = rep.getValue(LightConfig.NAME_KEY_SIM);
                setDeviceName(deviceName);
            }
            if (rep.hasAttribute(LightConfig.NAME_KEY)) {
                deviceName = rep.getValue(LightConfig.NAME_KEY);
                setDeviceName(deviceName);
            }
            if (rep.hasAttribute(Switch.VALUE_KEY)) {
                powerOn = rep.getValue(Switch.VALUE_KEY);
            }
            if (rep.hasAttribute(Brightness.BRIGHTNESS_KEY)) {
                brightness = rep.getValue(Brightness.BRIGHTNESS_KEY);
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
            if (resourceLinks != null) {
                rep.setValue(RTS_KEY, collectionResTypes);
                OcRepresentation[] links = resourceLinks.getOcRepresentation();
                rep.setValue(LINKS_KEY, links);

            } else {
                rep.setValue(LightConfig.NAME_KEY, deviceName);
                rep.setValue(LightConfig.NAME_KEY_SIM, deviceName);

                rep.setValue(Switch.VALUE_KEY, powerOn);

                brightness = Math.max(0, brightness);
                brightness = Math.min(100, brightness);
                rep.setValue(Brightness.BRIGHTNESS_KEY, brightness);
            }

        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to set representation values");
        }

        return rep;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean getPowerOn() {
        return powerOn;
    }

    public int getBrightness() {
        return brightness;
    }

    @Override
    public void update(boolean powerOn, int brightness) {
        this.powerOn = powerOn;
        this.brightness = brightness;

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
        if (resourceLinks != null) {
            return "[" + super.toString() + ", " + resourceLinks + "]";

        } else {
            return "[" + super.toString() + ", " + LightConfig.NAME_KEY + ": " + deviceName + ", "
                       + Switch.VALUE_KEY + ": " + powerOn + ", " + Brightness.BRIGHTNESS_KEY + ": " + brightness + "]";
        }
    }
}
