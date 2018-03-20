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
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * OcfLightDevice
 */
public class OcfLightDevice {

    static Light light;

    static boolean isTest;
    static int testFrequency = 3;

    static boolean useLinks = true;
    static String name;
    static boolean powerOn = true;
    static int brightness = 100;

    static OcResourceHandle deviceResourceHandle;

    public static void main(String args[]) throws IOException, InterruptedException {

        if ((args.length > 0) && (args[0].startsWith("-"))) {
            if (args[0].startsWith("-t")) {
                // toggle test
                isTest = true;
                try {
                    testFrequency = Integer.valueOf(args[0].substring(2));
                } catch (NumberFormatException e) {
                    msg("Frequency must be an integer in the range (1, 60), using default 3.");
                }
                testFrequency = Math.max(1, testFrequency);
                testFrequency = Math.min(60, testFrequency);
                msg("Server in test mode with frequecy of " + testFrequency);
            } else if (args[0].equalsIgnoreCase("-v")) {
                // vendor specific resource
                useLinks = false;
                msg("Server using vendor resource type");
            } else {
                msg("Unknown runtime parameter: " + args[0]);
            }
            parseNameAndInitialSettings(args, 1);

        } else {
            parseNameAndInitialSettings(args, 0);
        }

        PlatformConfig platformConfig = new PlatformConfig(ServiceType.IN_PROC, ModeType.SERVER, "0.0.0.0", 0,
                QualityOfService.LOW, "/tmp/server_security.dat", "/tmp/server_introspection.dat");

        OcPlatform.Configure(platformConfig);
        msg("Platform configured");

        try {
            deviceResourceHandle = OcPlatform.getResourceHandleAtUri(OcPlatform.WELL_KNOWN_DEVICE_QUERY);
            if (deviceResourceHandle != null) {
                OcPlatform.bindTypeToResource(deviceResourceHandle, Light.RES_TYPE);
            }

        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to bind device type to /oic/d resource");
            e.printStackTrace();
        }

        String uuid = null;
        if (NamesPropertyFile.getInstance().hasName(name)) {
            uuid = NamesPropertyFile.getInstance().getUuidForName(name);
        } else {
            uuid = UUID.randomUUID().toString();
            NamesPropertyFile.getInstance().updateNamesProperty(name, uuid);
        }

        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            frame.setIconImage(ImageIO.read(OcfLightDevice.class.getResource("/res/bulb-icon-32x32.png")));
        } catch (IOException ioe) {
            msgError("Error loading application icon: " + ioe.toString());
            ioe.printStackTrace();
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (light != null) {
//                    light.unregister();
                }
                msg("Shutdown");
                e.getWindow().dispose();
                System.exit(0);
            }
        });

        frame.setResizable(false);
        frame.setLayout(new FlowLayout());

        LightPanel lightPanel = new LightPanel(powerOn, brightness);
        light = new Light(useLinks, name, uuid, powerOn, brightness, lightPanel);

        frame.setContentPane(lightPanel);
        frame.pack();
        frame.setVisible(true);

        if (isTest) {
            // Start running a task to toggle the light and change its brightness
            Timer timer = new Timer();
            timer.schedule(new ToggleTask(lightPanel), testFrequency * 1000, testFrequency * 1000);
        }
    }

    private static void parseNameAndInitialSettings(String args[], int index) {
        if (args.length > index) {
            name = args[index];
        }

        if (args.length > index + 1) {
            String arg = args[index + 1];
            if (!arg.isEmpty()) {
                powerOn = arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("yes")
                        || arg.equals("1");
            }
        }

        if (args.length > index + 2) {
            try {
                brightness = Integer.valueOf(args[index + 2]);
            } catch (NumberFormatException e) {
                msg("Brightness must be an integer in the range (0, 100), using default 100.");
            }

            brightness = Math.max(0, brightness);
            brightness = Math.min(100, brightness);
        }

        if (name == null || name.isEmpty()) {
            name = "Light " + (System.currentTimeMillis() % 10000);
        }
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    public static class ToggleTask extends TimerTask {
        LightPanel lightPanel;

        public ToggleTask(LightPanel lightPanel) {
            this.lightPanel = lightPanel;
        }

        @Override
        public void run() {
            boolean powerOn = ! light.getPowerOn();
            int brightness = (light.getBrightness() + 10) % 101;
            OcfLightDevice.msg("Notifying observers for resource " + light.toString());
            light.update(powerOn, brightness);
            lightPanel.update(light, null);
        }
    }
}
