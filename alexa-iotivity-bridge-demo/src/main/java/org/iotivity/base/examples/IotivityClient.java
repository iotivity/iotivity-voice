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
import org.iotivity.base.ObserveType;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;
import org.iotivity.base.examples.ConnectedThing.LightDevice;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IotivityClient
 *
 * IotivityClient provides interaction with the IoTivity client stack.
 */
public class IotivityClient implements
        OcPlatform.OnResourceFoundListener,
        OcPlatform.OnResourcesFoundListener,
        OcResource.OnGetListener,
        OcResource.OnPutListener,
        OcResource.OnPostListener,
        OcResource.OnObserveListener {

    private final Map<String, OcResource> mIotivityResourceLookup = new ConcurrentHashMap<>();
    private final Map<String, Resource> mResourceLookup = new ConcurrentHashMap<>();
    private final Map<String, ConnectedThing.LightDevice> mConnectedThingLookup = new ConcurrentHashMap<>();
    private final Map<String, Long> mStaleResourceUriLookup = new ConcurrentHashMap<>();
    private final Comparator<ConnectedThing.LightDevice> nameComparator = new DeviceNameComparator();

    private ConnectedThing mConnectedThing;

    public IotivityClient() {
        // Start running a task to collect stale resources (runs every 10 seconds)
        Timer timer = new Timer();
        timer.schedule(new StaleResourcePurgeTask(), 10*1000, 10*1000);
    }

    /**
     * An event handler to be executed whenever a "findResource" request completes successfully
     *
     * @param ocResource found resource
     */
    @Override
    public synchronized void onResourceFound(OcResource ocResource) {
        if (null == ocResource) {
            AlexaIotivityBridgeDemo.msgError("Found resource is invalid");
            return;
        }

        // Get the resource uri
        String resourceUri = ocResource.getUri();
        // Get the resource host address
        String hostAddress = ocResource.getHost();

        boolean tracked = false;

        // For now, we are only interested in known light resources
        if (resourceUri.startsWith(Light.UPNP_OIC_URI_PREFIX_LIGHT)
                || resourceUri.startsWith(Light.OCF_OIC_URI_PREFIX_LIGHT)
                || resourceUri.startsWith(Light.OIC_URI_PREFIX_LIGHT)) {
            if (!mResourceLookup.containsKey(resourceUri)) {

                AlexaIotivityBridgeDemo.msg("URI of the new light resource: " + resourceUri);
//                AlexaIotivityBridgeDemo.msg("Host address of the new light resource: " + hostAddress);

                Light light = new Light();
                light.setUri(resourceUri);

                mResourceLookup.put(resourceUri, light);
            }

            // Call a local method which will internally invoke "observe" API on the found resource
            observeFoundResource(ocResource);

            // For OCF devices, the name is the 'n' property of the device
            if (resourceUri.startsWith(Light.OCF_OIC_URI_PREFIX_LIGHT)
                    || resourceUri.startsWith(Light.OIC_URI_PREFIX_LIGHT)) {
                Light light = (Light) mResourceLookup.get(resourceUri);
                OcPlatform.OnDeviceFoundListener deviceFoundListener = new DeviceFoundListener(light);
                try {
                    OcPlatform.getDeviceInfo(hostAddress, OcPlatform.WELL_KNOWN_DEVICE_QUERY,
                            EnumSet.of(OcConnectivityType.CT_DEFAULT), deviceFoundListener);
                } catch (OcException e) {
                    AlexaIotivityBridgeDemo.msgError(e.toString());
                }
            }

            mIotivityResourceLookup.put(resourceUri, ocResource);
            tracked = true;

        } else {
            if (!resourceUri.equals("/oic/d")) {
                AlexaIotivityBridgeDemo.msg("URI of the new (for now, untracked) resource: " + resourceUri);
            }
        }

        if (tracked) {
            // Call a local method which will internally invoke "get" API on the found resource
            getResourceRepresentation(ocResource);
        }
    }

    @Override
    public synchronized void onFindResourceFailed(Throwable throwable, String uri) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Uri: " + uri + " Error code: " + errCode);
        }

        AlexaIotivityBridgeDemo.msgError("Find resource failed");
    }

    /**
     * Resource found listener specifically for links.
     */
    class ResourceFoundListener implements OcPlatform.OnResourceFoundListener {

        private String mParentUri;
        private String mHref; // expected link uri

        ResourceFoundListener(String resourceUri, String href) {
            mParentUri = resourceUri;
            mHref = href;
        }

        public synchronized void onResourceFound(OcResource ocResource) {
            if (null == ocResource) {
                AlexaIotivityBridgeDemo.msgError("Found resource is invalid");
                return;
            }

            String resourceUri = ocResource.getUri();

            boolean tracked = false;

            if (resourceUri.equalsIgnoreCase(mHref)) {
                if (!mResourceLookup.containsKey(resourceUri)) {
                    AlexaIotivityBridgeDemo.msg("URI of the new linked resource: " + resourceUri);

                    if (resourceUri.startsWith(BinarySwitch.UPNP_OIC_URI_PREFIX_BINARY_SWITCH)
                            || resourceUri.startsWith(BinarySwitch.OCF_OIC_URI_PREFIX_BINARY_SWITCH)) {
                        BinarySwitch binarySwitch = new BinarySwitch();
                        binarySwitch.setUri(resourceUri);

                        // Update the device
                        Light light = (Light) mResourceLookup.get(mParentUri);
                        if (light != null) {
                            light.setBinarySwitch(binarySwitch);
                            mResourceLookup.put(resourceUri, binarySwitch);
                            tracked = true;
                        }

                    } else if (resourceUri.startsWith(Brightness.UPNP_OIC_URI_PREFIX_BRIGHTNESS)
                            || resourceUri.startsWith(Brightness.OCF_OIC_URI_PREFIX_BRIGHTNESS)) {
                        Brightness brightness = new Brightness();
                        brightness.setUri(resourceUri);

                        // Update the device
                        Light light = (Light) mResourceLookup.get(mParentUri);
                        if (light != null) {
                            light.setBrightness(brightness);
                            mResourceLookup.put(resourceUri, brightness);
                            tracked = true;
                        }

                    } else if (resourceUri.startsWith(Configuration.OCF_OIC_URI_PREFIX_CONFIG)) {
                        Configuration config = new Configuration();
                        config.setUri(resourceUri);

                        // Update the device
                        Light light = (Light) mResourceLookup.get(mParentUri);
                        if (light != null) {
                            light.setConfiguration(config);
                            mResourceLookup.put(resourceUri, config);
                            tracked = true;
                        }

                    } else {
                        // Unexpected resource
                        AlexaIotivityBridgeDemo.msg("URI of an unexpected resource: " + resourceUri);
                    }

                    mIotivityResourceLookup.put(resourceUri, ocResource);

                    if (tracked) {
                        // Call a local method which will internally invoke "get" API on the found resource
                        getResourceRepresentation(ocResource);

                        // Call a local method which will internally invoke "observe" API on the found resource
                        observeFoundResource(ocResource);
                    }
                }
            }
        }

        public synchronized void onFindResourceFailed(Throwable throwable, String uri) {
            if (throwable instanceof OcException) {
                OcException ocEx = (OcException) throwable;
                AlexaIotivityBridgeDemo.msgError(ocEx.toString());
                ErrorCode errCode = ocEx.getErrorCode();
                // do something based on errorCode
                AlexaIotivityBridgeDemo.msgError("Uri: " + uri + " Error code: " + errCode);
            }

            AlexaIotivityBridgeDemo.msgError("Find resource failed");
        }
    }

    /**
     * An event handler to be executed whenever a "findResources" request
     * completes successfully
     *
     * @param ocResources
     *            array of found resources
     */
    @Override
    public synchronized void onResourcesFound(OcResource[] ocResources) {
        if (null == ocResources) {
            AlexaIotivityBridgeDemo.msgError("Found resources is invalid");
            return;
        }

        for (OcResource ocResource : ocResources) {
            onResourceFound(ocResource);
        }
    }

    @Override
    public synchronized void onFindResourcesFailed(Throwable throwable, String uri) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Uri: " + uri + " Error code: " + errCode);
        }

        AlexaIotivityBridgeDemo.msgError("Find resource failed");
    }

    /**
     * Local method to get representation of a found resource
     *
     * @param ocResource found resource
     */
    private void getResourceRepresentation(OcResource ocResource) {

        Map<String, String> queryParams = new HashMap<>();
        try {
            // Invoke resource's "get" API with a OcResource.OnGetListener event listener implementation
            ocResource.get(queryParams, this);

        } catch (OcException e) {
            AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"get\" API -- " + e.toString());
        }
    }

    /**
     * An event handler to be executed whenever a "get" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     */
    @Override
    public synchronized void onGetCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation) {

        try {
            // Read attribute values into local representation of resource
            final String ocRepUri = ocRepresentation.getUri();
            if (ocRepUri != null && !ocRepUri.isEmpty()) {
//                AlexaIotivityBridgeDemo.msg("Get Resource URI: " + ocRepUri);

                Resource resource = mResourceLookup.get(ocRepUri);
                if (resource != null) {
                    resource.setOcRepresentation(ocRepresentation);
//                    AlexaIotivityBridgeDemo.msg("Get Resource attributes: " + resource.toString());

                    if (resource instanceof Device) {
                        Device device = (Device) resource;
                        Links links = device.getLinks();
                        for (Link link : links.getLinks()) {
                            String href = link.getHref();
                            if (!href.equals("/oic/d")) {
                                // rt could be String or String[]
                                Object rt = link.getRt();
                                String rtAsString = null;
                                if (rt instanceof String) {
                                    rtAsString = (String) rt;

                                } else if (rt instanceof String[]) {
                                    if (((String[]) rt).length > 0) {
                                        rtAsString = ((String[]) rt)[0];
                                    } else {
                                        AlexaIotivityBridgeDemo.msgError("(String[])rt is empty");
                                    }

                                } else {
                                    AlexaIotivityBridgeDemo.msgError("Unknown rt type of " + rt.getClass().getName());
                                }

                                if ((rtAsString != null) && (!mResourceLookup.containsKey(href))) {
                                    AlexaIotivityBridgeDemo.msg("Finding all resources of type " + rtAsString);
                                    String requestUri = OcPlatform.WELL_KNOWN_QUERY + "?rt=" + rtAsString;
                                    OcPlatform.findResource("", requestUri, EnumSet.of(OcConnectivityType.CT_DEFAULT), new ResourceFoundListener(ocRepUri, href));
                                }
                            }
                        }

                        if (resource instanceof Light) {
                            Light light = (Light) resource;

                            if (((light.getBinarySwitch() != null) && (light.getBinarySwitch().isInitialized())
                                    && (light.getBrightness() != null) && light.getBrightness().isInitialized())
                                    || (!light.hasLinksProperty())) {

                                ConnectedThing.LightDevice lightDevice = mConnectedThingLookup.get(light.getUri());
                                if (lightDevice == null) {
                                    lightDevice = new ConnectedThing.LightDevice();
                                    lightDevice.setUri(light.getUri());
                                }
                                lightDevice.setName(light.getName());
                                lightDevice.setPowerOn(light.getState());
                                lightDevice.setBrightness(light.getLightLevel());
                                mConnectedThingLookup.put(light.getUri(), lightDevice);
                                mStaleResourceUriLookup.put(light.getUri(), System.currentTimeMillis());

                                // publish updates to connected thing
                                ConnectedThing.LightDevice[] lightDevices = mConnectedThingLookup.values().toArray(new ConnectedThing.LightDevice[0]);
                                Arrays.sort(lightDevices, nameComparator);
                                mConnectedThing.setLightDevices(lightDevices);
//                                AlexaIotivityBridgeDemo.publishUpdatePayload(toUpdatePayload(lightDevices));
                            }

                        } else {
                            // TODO: handle additional devices
                        }
                    }

                } else {
                    AlexaIotivityBridgeDemo.msgError("Get No resource for uri " + ocRepUri);
                }

            } else {
                AlexaIotivityBridgeDemo.msgError("Get No Resource URI");
            }

        } catch (OcException e) {
            AlexaIotivityBridgeDemo.msgError("Get Failed to read the attributes of resource -- " + e.toString());
        }
    }

    /**
     * An event handler to be executed whenever a "get" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onGetFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Error code: " + errCode);
        }
        AlexaIotivityBridgeDemo.msgError("Failed to get representation of a found light resource");
    }

    /**
     * Set state for a light resource
     */
    public void updateLight(String uri, boolean newState, int newLightLevel) {
        ConnectedThing.LightDevice lightDevice = mConnectedThingLookup.get(uri);
        if (lightDevice != null) {
            OcResource ocResource = mIotivityResourceLookup.get(uri);
            if (ocResource != null) {
                putLightRepresentation(ocResource, newState, newLightLevel);
            }
        }
    }

    /**
     * Rename a light resource
     */
    public void updateLight(String uri, String newName) {
        ConnectedThing.LightDevice lightDevice = mConnectedThingLookup.get(uri);
        if (lightDevice != null) {
            OcResource ocResource = mIotivityResourceLookup.get(uri);
            if (ocResource != null) {
                putLightRepresentation(ocResource, newName);
            }
        }
    }

    /**
     * Local method to put a different name for this light resource
     */
    private void putLightRepresentation(OcResource ocResource, String newName) {
        final String resourceUri = ocResource.getUri();

        Light light = (Light) mResourceLookup.get(resourceUri);
        if (light != null && resourceUri.startsWith(Light.OCF_OIC_URI_PREFIX_LIGHT)) {
            if (light.hasLinksProperty()) {
                final Configuration config = light.getConfiguration();
                if ((config != null) && (config.isInitialized())) {
                    config.setName(newName);
                    OcRepresentation configRepresentation = null;
                    try {
                        configRepresentation = config.getOcRepresentation();

                    } catch (OcException e) {
                        AlexaIotivityBridgeDemo.msgError("Failed to get OcRepresentation from a configuration -- " + e.toString());
                    }

                    if (configRepresentation != null) {
                        Map<String, String> queryParams = new HashMap<>();
                        try {
                            // Invoke resource's "put" API with a new representation
                            OcResource configResource = mIotivityResourceLookup.get(config.getUri());
                            if (configResource != null) {
                                configResource.put(configRepresentation, queryParams, this);

                            } else {
                                AlexaIotivityBridgeDemo.msgError("No configuration for light uri " + resourceUri);
                            }

                        } catch (OcException e) {
                            AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"put\" API -- " + e.toString());
                        }
                    }

                } else {
                    if (config == null) {
                        AlexaIotivityBridgeDemo.msgError("No configuration for light uri " + resourceUri);

                    } else {
                        AlexaIotivityBridgeDemo.msgError("No configuration (initialized) for light uri " + resourceUri);
                    }
                }

            } else {
                // properties are on the device
                light.setName(newName);

                OcRepresentation lightRepresentation = null;
                try {
                    lightRepresentation = light.getOcRepresentation();

                } catch (OcException e) {
                    AlexaIotivityBridgeDemo.msgError("Failed to get OcRepresentation from light -- " + e.toString());
                }

                if (lightRepresentation != null) {
                    Map<String, String> queryParams = new HashMap<>();
                    try {
                        // Invoke resource's "put" API with a new representation
                        ocResource.put(lightRepresentation, queryParams, this);

                    } catch (OcException e) {
                        AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"put\" API -- " + e.toString());
                    }
                }
            }

        } else if (!resourceUri.startsWith(Light.OCF_OIC_URI_PREFIX_LIGHT)) {
//            AlexaIotivityBridgeDemo.msg("No configuration available for light uri " + resourceUri);

        } else {
            AlexaIotivityBridgeDemo.msgError("No light for uri " + resourceUri);
        }
    }

    /**
     * Local method to put a different state for this light resource
     */
    private void putLightRepresentation(OcResource ocResource, boolean newState, int newLightLevel) {
        final String resourceUri = ocResource.getUri();

        Light light = (Light) mResourceLookup.get(resourceUri);
        if (light != null) {
            // set new values
            if (light.hasLinksProperty()) {
                // actually, set directly on the service (avoid possible conflict if auto discover is running)
                // light.setState(newState);
                // light.setLightLevel(newLightLevel);

                final BinarySwitch binarySwitch = light.getBinarySwitch();
                if ((binarySwitch != null) && (binarySwitch.isInitialized())) {
                    binarySwitch.setValue(newState);
                    OcRepresentation binarySwitchRepresentation = null;
                    try {
                        binarySwitchRepresentation = binarySwitch.getOcRepresentation();

                    } catch (OcException e) {
                        AlexaIotivityBridgeDemo.msgError("Failed to get OcRepresentation from a binary switch -- " + e.toString());
                    }

                    if (binarySwitchRepresentation != null) {
                        Map<String, String> queryParams = new HashMap<>();
                        try {
                            // Invoke resource's "put" (or "post") API with a new representation
                            OcResource binarySwitchResource = mIotivityResourceLookup.get(binarySwitch.getUri());
                            if (binarySwitchResource != null) {
                                if (binarySwitchResource.getUri().startsWith(BinarySwitch.UPNP_OIC_URI_PREFIX_BINARY_SWITCH)) {
                                    // upnp bridge requires 'post'
                                    binarySwitchResource.post(binarySwitchRepresentation, queryParams, this);
                                } else {
                                    binarySwitchResource.put(binarySwitchRepresentation, queryParams, this);
                                }

                            } else {
                                AlexaIotivityBridgeDemo.msgError("No binary switch for light uri " + resourceUri);
                            }

                        } catch (OcException e) {
                            AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"put\" API -- " + e.toString());
                        }
                    }

                } else {
                    if (binarySwitch == null) {
                        AlexaIotivityBridgeDemo.msgError("No binary switch for light uri " + resourceUri);

                    } else {
                        AlexaIotivityBridgeDemo.msgError("No binary switch (initialized) for light uri " + resourceUri);
                    }
                }

                Brightness brightness = light.getBrightness();
                if ((brightness != null) && (brightness.isInitialized())) {
                    brightness.setBrightness(newLightLevel);
                    OcRepresentation brightnessRepresentation = null;
                    try {
                        brightnessRepresentation = brightness.getOcRepresentation();

                    } catch (OcException e) {
                        AlexaIotivityBridgeDemo.msgError("Failed to get OcRepresentation from a brightness -- " + e.toString());
                    }

                    if (brightnessRepresentation != null) {
                        Map<String, String> queryParams = new HashMap<>();
                        try {
                            // Invoke resource's "put" (or "post") API with a new representation
                            OcResource brightnessResource = mIotivityResourceLookup.get(brightness.getUri());
                            if (brightnessResource != null) {
                                if (brightnessResource.getUri().startsWith(Brightness.UPNP_OIC_URI_PREFIX_BRIGHTNESS)) {
                                    // upnp bridge requires 'post'
                                    brightnessResource.post(brightnessRepresentation, queryParams, this);
                                } else {
                                    brightnessResource.put(brightnessRepresentation, queryParams, this);
                                }

                            } else {
                                AlexaIotivityBridgeDemo.msgError("No brightness for light uri " + resourceUri);
                            }

                        } catch (OcException e) {
                            AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"put\" API -- " + e.toString());
                        }
                    }

                } else {
                    if (brightness == null) {
                        AlexaIotivityBridgeDemo.msgError("No brightness for light uri " + resourceUri);

                    } else {
                        AlexaIotivityBridgeDemo.msgError("No brightness (initialized) for light uri " + resourceUri);
                    }
                }

            } else {
                // properties are on the device
                light.setState(newState);
                light.setLightLevel(newLightLevel);

                OcRepresentation lightRepresentation = null;
                try {
                    lightRepresentation = light.getOcRepresentation();

                } catch (OcException e) {
                    AlexaIotivityBridgeDemo.msgError("Failed to get OcRepresentation from light -- " + e.toString());
                }

                if (lightRepresentation != null) {
                    Map<String, String> queryParams = new HashMap<>();
                    try {
                        // Invoke resource's "put" API with a new representation
                        ocResource.put(lightRepresentation, queryParams, this);

                    } catch (OcException e) {
                        AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"put\" API -- " + e.toString());
                    }
                }
            }

        } else {
            AlexaIotivityBridgeDemo.msgError("No light for uri " + resourceUri);
        }
    }

    /**
     * An event handler to be executed whenever a "put" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     */
    @Override
    public synchronized void onPutCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation) {

        try {
            // Read attribute values into local representation of resource
            final String ocRepUri = ocRepresentation.getUri();
            if (ocRepUri != null && !ocRepUri.isEmpty()) {
//                AlexaIotivityBridgeDemo.msg("Put Resource URI: " + ocRepUri);

                Resource resource = mResourceLookup.get(ocRepUri);
                if (resource != null) {
                    resource.setOcRepresentation(ocRepresentation);
                    AlexaIotivityBridgeDemo.msg("Put Resource attributes: " + resource.toString());

                } else {
                    AlexaIotivityBridgeDemo.msgError("Put No resource for uri " + ocRepUri);
                }

            } else {
                AlexaIotivityBridgeDemo.msgError("Put No Resource URI");
            }

        } catch (OcException e) {
            AlexaIotivityBridgeDemo.msgError("Put Failed to create resource representation -- " + e.toString());
        }
    }

    /**
     * An event handler to be executed whenever a "put" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onPutFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Error code: " + errCode);
        }
        AlexaIotivityBridgeDemo.msgError("Failed to \"put\" a new representation");
    }

    /**
     * An event handler to be executed whenever a "post" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     */
    @Override
    public synchronized void onPostCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation) {

        try {
            // Read attribute values into local representation of resource
            final String ocRepUri = ocRepresentation.getUri();
            if (ocRepUri != null && !ocRepUri.isEmpty()) {
//                AlexaIotivityBridgeDemo.msg("Post Resource URI: " + ocRepUri);

                Resource resource = mResourceLookup.get(ocRepUri);
                if (resource != null) {
                    resource.setOcRepresentation(ocRepresentation);
                    AlexaIotivityBridgeDemo.msg("Post Resource attributes: " + resource.toString());

                } else {
                    AlexaIotivityBridgeDemo.msgError("Post No resource for uri " + ocRepUri);
                }

            } else {
                AlexaIotivityBridgeDemo.msgError("Post No Resource URI");
            }

        } catch (OcException e) {
            AlexaIotivityBridgeDemo.msgError("Post Failed to create resource representation -- " + e.toString());
        }
    }

    /**
     * An event handler to be executed whenever a "post" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onPostFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Error code: " + errCode);
        }
        AlexaIotivityBridgeDemo.msgError("Failed to \"post\" a new representation");
    }

    /**
     * Local method to start observing this resource
     *
     * @param ocResource found resource
     */
    private void observeFoundResource(OcResource ocResource) {
        try {
            // Invoke resource's "observe" API with a observe type
            ocResource.observe(ObserveType.OBSERVE, new HashMap<String, String>(), this);

        } catch (OcException e) {
            AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"observe\" API -- " + e.toString());
        }
    }

    /**
     * An event handler to be executed whenever a "observe" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     * @param sequenceNumber   sequence number
     */
    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation, int sequenceNumber) {
//        if (OcResource.OnObserveListener.REGISTER == sequenceNumber) {
//            AlexaIotivityBridgeDemo.msg("Observe registration action is successful");
//        } else {
//            AlexaIotivityBridgeDemo.msg("Observe sequence number " + sequenceNumber);
//        }

        if ((sequenceNumber > 0) && (sequenceNumber < (OcResource.OnObserveListener.MAX_SEQUENCE_NUMBER + 1))) {
            onGetCompleted(list, ocRepresentation);
        }
    }

    /**
     * An event handler to be executed whenever a "observe" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onObserveFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            AlexaIotivityBridgeDemo.msgError(ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            // do something based on errorCode
            AlexaIotivityBridgeDemo.msgError("Error code: " + errCode);
        }
        AlexaIotivityBridgeDemo.msgError("Observation of the found light resource has failed");
    }

    public void setConnectedThing(ConnectedThing connectedThing) {
        mConnectedThing = connectedThing;
    }

    private String toUpdatePayload(ConnectedThing.LightDevice[] lightDevices) {
        StringBuffer payload = new StringBuffer();
        payload.append("{\"state\":{\"reported\":{\"lightDevices\":[");

        int index = 0;
        for (LightDevice lightDevice : lightDevices) {
            payload.append("{\"name\":\"" + lightDevice.getName() + "\",").
            append("\"uri\":\"" + lightDevice.getUri() + "\",").
            append("\"powerOn\":" + lightDevice.getPowerOn() + ",").
            append("\"brightness\":" + lightDevice.getBrightness() + "}");

            if (++index < lightDevices.length) {
                payload.append(",");
            }
        }
        payload.append("]}}}");
        return payload.toString();
    }

    public synchronized void cancelObserve() {
        for (OcResource ocResource : mIotivityResourceLookup.values()) {
            try {
                AlexaIotivityBridgeDemo.msg("Cancelling Observe for " + ocResource.getUri());
                ocResource.cancelObserve();
            } catch (OcException e) {
                AlexaIotivityBridgeDemo.msgError("Error occurred while invoking \"cancelObserve\" API for resource "
                        + ocResource.getUri() + " -- " + e.toString());
            }
        }
    }

    class DeviceFoundListener implements OcPlatform.OnDeviceFoundListener {

        Light light;

        public DeviceFoundListener(Light light) {
            this.light = light;
        }

        @Override
        public void onDeviceFound(OcRepresentation ocRepresentation) {
            try {
                if (ocRepresentation.hasAttribute("n")) {
                    String name = ocRepresentation.getValue("n");
                    light.setName(name);
//                    AlexaIotivityBridgeDemo.msg("OCF device found callback: 'n' = " + name);
                } else {
                    AlexaIotivityBridgeDemo.msgError("'n' attribute not found for device " + light.toString());
                }

            } catch (OcException e) {
                AlexaIotivityBridgeDemo.msgError("Get 'n' attribute failed for device " + light.toString());
                e.printStackTrace();
            }
        }
    };

    class DeviceNameComparator implements Comparator<ConnectedThing.LightDevice> {
        @Override
        public int compare(ConnectedThing.LightDevice lhs, ConnectedThing.LightDevice rhs) {
            return (lhs.getName().compareToIgnoreCase(rhs.getName()));
        }
    }

    public class StaleResourcePurgeTask extends TimerTask {
        @Override
        public void run() {
            try {
                boolean mustDeleteShadowDocument = false;
                long now = System.currentTimeMillis();
                for (Map.Entry<String,Long> entry : mStaleResourceUriLookup.entrySet()) {
                    if (entry.getValue() < now - 30*1000) {
                        // uri not seen in 30 seconds, remove from maps
                        String key = entry.getKey();
                        AlexaIotivityBridgeDemo.msg("Removing stale uri " + key);
                        Light light = (Light) mResourceLookup.get(key);
                        for (Link link : light.getLinks().getLinks()) {
                            AlexaIotivityBridgeDemo.msg("Removing stale uri link " + link.getHref());
                            mIotivityResourceLookup.remove(link.getHref());
                            mResourceLookup.remove(link.getHref());
                        }
                        mConnectedThingLookup.remove(key);
                        mIotivityResourceLookup.remove(key);
                        mResourceLookup.remove(key);
                        mStaleResourceUriLookup.remove(key);

                        mustDeleteShadowDocument = true;
                    }
                }
                if (mConnectedThingLookup.isEmpty()) {
                    mConnectedThing.setLightDevices(new ConnectedThing.LightDevice[0]);
                    mustDeleteShadowDocument = true;
                }
                if (mustDeleteShadowDocument) {
                    mConnectedThing.delete();
                }

            } catch (Exception e) {
                AlexaIotivityBridgeDemo.msgError("Error running StaleResourcePurgeTask: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
