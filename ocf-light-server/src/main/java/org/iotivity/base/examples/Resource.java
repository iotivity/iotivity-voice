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

import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.ErrorCode;
import org.iotivity.base.ObservationInfo;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.RequestHandlerFlag;
import org.iotivity.base.RequestType;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;
import java.util.Map;
import java.util.Observable;

/**
 * Resource
 *
 * This is the base class for all resources
 */

abstract public class Resource extends Observable implements OcPlatform.EntityHandler {

    private String resUri;
    private OcResourceHandle resHandle;

    public Resource(String uri, String resType, String resIf) {
        resUri = uri;

        try {
            resHandle = OcPlatform.registerResource(resUri, resType, resIf, this,
                    EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE, ResourceProperty.SECURE));

        } catch (OcException e) {
            OcfLightDevice.msgError("Failed to create resource " + resUri);
            e.printStackTrace();
        }
    }

    @Override
    public synchronized EntityHandlerResult handleEntity(final OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        if (null == request) {
            OcfLightDevice.msg("Server request is invalid");
            return ehResult;
        }
        // Get the request flags
        EnumSet<RequestHandlerFlag> requestFlags = request.getRequestHandlerFlagSet();
        if (requestFlags.contains(RequestHandlerFlag.INIT)) {
            OcfLightDevice.msg("Request Flag: Init for " + request.getResourceUri());
            ehResult = EntityHandlerResult.OK;
        }
        if (requestFlags.contains(RequestHandlerFlag.REQUEST)) {
//            OcfLightDevice.msg("Request Flag: Request for " + request.getResourceUri());
            ehResult = handleRequest(request);
        }
        if (requestFlags.contains(RequestHandlerFlag.OBSERVER)) {
//            OcfLightDevice.msg("Request Flag: Observer for " + request.getResourceUri());
            ehResult = handleObserver(request);
        }
        return ehResult;
    }

    private EntityHandlerResult handleRequest(OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        // Check for query params (if any)
        Map<String, String> queries = request.getQueryParameters();
//        if (!queries.isEmpty()) {
//            OcfLightDevice.msg("Query processing is up to entityHandler");
//        } else {
//            OcfLightDevice.msg("No query parameters in this request");
//        }

        for (Map.Entry<String, String> entry : queries.entrySet()) {
//            OcfLightDevice.msg("Query key: " + entry.getKey() + " value: " + entry.getValue());
        }

        // Get the request type
        RequestType requestType = request.getRequestType();
        switch (requestType) {
        case GET:
//            OcfLightDevice.msg("Request Type is GET for " + request.getResourceUri());
            ehResult = handleGetRequest(request);
            break;
        case PUT:
//            OcfLightDevice.msg("Request Type is PUT for " + request.getResourceUri());
            ehResult = handlePutRequest(request);
            break;
        case POST:
//            OcfLightDevice.msg("Request Type is POST for " + request.getResourceUri());
            ehResult = handlePutRequest(request); // same as put
            break;
        case DELETE:
            OcfLightDevice.msg("Request Type is DELETE for " + request.getResourceUri());
            ehResult = handleDeleteRequest();
            break;
        }
        return ehResult;
    }

    private EntityHandlerResult handleGetRequest(final OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        response.setResponseResult(EntityHandlerResult.OK);
        response.setResourceRepresentation(getOcRepresentation());
        return sendResponse(response);
    }

    private EntityHandlerResult handlePutRequest(final OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        setOcRepresentation(request.getResourceRepresentation());
        response.setResourceRepresentation(getOcRepresentation());
        response.setResponseResult(EntityHandlerResult.OK);

        // notify on separate thread
        Thread observerNotifier = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore
                }

                notifyObservers(request);

                // notify observers (ie implementors of Observer)
                setChanged();
                notifyObservers();
            }
        });
        observerNotifier.setDaemon(true);
        observerNotifier.start();

        return sendResponse(response);
    }

    private EntityHandlerResult handleDeleteRequest() {
        try {
            unregisterResource();
            return EntityHandlerResult.RESOURCE_DELETED;
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to unregister resource " + resUri);
            return EntityHandlerResult.ERROR;
        }
    }

    private EntityHandlerResult handleObserver(final OcResourceRequest request) {
        ObservationInfo observationInfo = request.getObservationInfo();
        switch (observationInfo.getObserveAction()) {
        case REGISTER:
//            OcfLightDevice.msg("handleObserver register observer " + String.format("%02x", observationInfo.getOcObservationId()));
            break;
        case UNREGISTER:
//            OcfLightDevice.msg("handleObserver unregister observer " + String.format("%02x", observationInfo.getOcObservationId()));
            break;
        }
        return EntityHandlerResult.OK;
    }

    private void notifyObservers(OcResourceRequest request) {
        OcfLightDevice.msg("Notifying observers for resource " + this);
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

    private EntityHandlerResult sendResponse(OcResourceResponse response) {
        try {
            OcPlatform.sendResponse(response);
            return EntityHandlerResult.OK;
        } catch (OcException e) {
            OcfLightDevice.msgError(e.toString());
            OcfLightDevice.msgError("Failed to send response");
            return EntityHandlerResult.ERROR;
        }
    }

    public synchronized void unregisterResource() throws OcException {
        if (null != resHandle) {
            OcPlatform.unregisterResource(resHandle);
            OcfLightDevice.msg("Unregistered resource " + resUri);
        }
    }

    public abstract void setOcRepresentation(OcRepresentation rep);

    public abstract OcRepresentation getOcRepresentation();

    public String getResourceUri() {
        return resUri;
    }

    public OcResourceHandle getResourceHandle() {
        return resHandle;
    }

    @Override
    public String toString() {
        return resUri;
    }
}
