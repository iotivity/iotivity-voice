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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;

/**
 * IotivityScanner
 */
public class IotivityScanner {

    private static IotivityScannerClient iotivityClient;

    /**
     * Configure and initialize platform.
     */
    private static void startIotivityClient(int frequency) {

        PlatformConfig platformConfig = new PlatformConfig(
                ServiceType.IN_PROC,
                ModeType.CLIENT,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );
        msg("Configuring platform.");
        OcPlatform.Configure(platformConfig);

        iotivityClient = new IotivityScannerClient(frequency);
    }

    public static void main(String args[]) throws IOException, InterruptedException {

        int frequency = 10; // seconds;

        if (args.length > 0) {
            try {
                frequency = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                msg("Frequency must be an integer in the range (1, 60), using default 10.");
            }

            frequency = Math.max(1, frequency);
            frequency = Math.min(60, frequency);
        }

        startIotivityClient(frequency); // creates iotivityClient

        while (true) {
            try {
                msg("Finding all resources of type " + Light.OIC_TYPE_DEVICE_LIGHT);
                String requestUri = OcPlatform.WELL_KNOWN_QUERY + "?rt=" + Light.OIC_TYPE_DEVICE_LIGHT;
                OcPlatform.findResources("", requestUri, EnumSet.of(OcConnectivityType.CT_DEFAULT), iotivityClient);

            } catch (OcException e) {
                msgError(e.toString());
                msgError("Failed to invoke find resource API");
            }

            sleep(frequency);

            iotivityClient.cancelObserve();
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
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
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
