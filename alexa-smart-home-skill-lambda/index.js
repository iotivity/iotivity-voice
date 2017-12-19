"use strict";

const AWS = require("aws-sdk");
const uuidV4 = require("uuid/v4");

// replace with endpoint from AWS IoT Device
const iotData = new AWS.IotData({endpoint:"a2a16j9xf0mmy6.iot.us-east-1.amazonaws.com", region:"us-east-1"});
// replace with Thing Name for AWS IoT Device
const thingName = "LeetoniaLinuxSystem";
// replace with skill's Application Id
const APP_ID = "amzn1.ask.skill.26df3d97-ba4d-44c4-aee5-0f20cce5375b";
const SKILL_NAME = "AlexaIotivityHomeSkill";

function generateMessageId() {
    return uuidV4();
}

function generateDevice(endpointId, friendlyName, uri) {
    return {
        endpointId: endpointId,
        manufacturerName: "Intel",
        friendlyName: friendlyName,
        description: uri,
        displayCategories: [ "LIGHT" ],
        cookie: {},
        capabilities: [
            {
                type: "AlexaInterface",
                interface: "Alexa",
                version: "3"
            },
            {
                type: "AlexaInterface",
                interface: "Alexa.EndpointHealth",
                version: "3",
                properties: {
                    supported: [
                        {
                            name: "connectivity"
                        }
                    ],
                    proactivelyReported: true,
                    retrievable: true
                }
            },
            {
                type: "AlexaInterface",
                interface: "Alexa.PowerController",
                version: "3",
                properties: {
                    supported: [
                        {
                            name: "powerState"
                        }
                    ],
                    proactivelyReported: true,
                    retrievable: true
                }
            },
            {
                type: "AlexaInterface",
                interface: "Alexa.BrightnessController",
                version: "3",
                properties: {
                    supported: [
                        {
                            name: "brightness"
                        }
                    ],
                    proactivelyReported: true,
                    retrievable: true
                }
            }
        ]
    };
}

function generateStateResponse(endpointId, correlationToken, powerOn, brightness) {
    const currentTime = new Date().toISOString();
    return {
        context: {
            properties: [
                {
                    namespace: "Alexa.EndpointHealth",
                    name: "connectivity",
                    value: {
                        value: "OK"
                    },
                    timeOfSample: currentTime,
                    uncertaintyInMilliseconds: 1000
                },
                {
                    namespace: "Alexa.PowerController",
                    name: "powerState",
                    value: powerOn ? "ON" : "OFF",
                    timeOfSample: currentTime,
                    uncertaintyInMilliseconds: 1000
                },
                {
                    namespace: "Alexa.BrightnessController",
                    name: "brightness",
                    value: brightness,
                    timeOfSample: currentTime,
                    uncertaintyInMilliseconds: 1000
                }
            ]
        },
        event: {
            header: {
                namespace: "Alexa",
                name: "StateReport",
                payloadVersion: "3",
                messageId: generateMessageId(),
                correlationToken: correlationToken
            },
            endpoint: {
                scope: {
                    type: "BearerToken",
                    token: "access-token-from-Amazon"
                },
                endpointId: endpointId
            },
            payload: {}
        }
    };
}

function generateResponse(namespace, name, value, endpointId, correlationToken) {
    return {
        context: {
            properties: [
                {
                    namespace: namespace,
                    name: name,
                    value: value,
                    timeOfSample: new Date().toISOString(),
                    uncertaintyInMilliseconds: 1000
                }
            ]
        },
        event: {
            header: {
                namespace: "Alexa",
                name: "Response",
                payloadVersion: "3",
                messageId: generateMessageId(),
                correlationToken: correlationToken
            },
            endpoint: {
                scope: {
                    type: "BearerToken",
                    token: "access-token-from-Amazon"
                },
                endpointId: endpointId
            },
            payload: {}
        }
    };
}

function generateErrorResponse(type, message, endpointId, correlationToken) {
    return {
        event: {
            header: {
                namespace: "Alexa",
                name: "ErrorResponse",
                payloadVersion: "3",
                messageId: generateMessageId(),
                correlationToken: correlationToken
            },
            endpoint: {
                scope: {
                    type: "BearerToken",
                    token: "access-token-from-Amazon"
                },
                endpointId: endpointId
            },
            payload: {
                type: type,
                message: message
            }
        }
    };
}

function isValidToken(userAccessToken) {
    // TODO:
    return true;
}

function isDeviceOnline(endpointId, userAccessToken) {
    // TODO:
    console.log("DEBUG", "isDeviceOnline -- endpointId: " + endpointId);
    return true;
}

function getDevicesFromAwsIot(userAccessToken, callback) {
    var devices = [];
    const thingNameParam = {
        thingName: thingName
    };

    iotData.getThingShadow(thingNameParam, function(err, data) {
        console.log("DEBUG", "get thing shadow");
        if (err) console.log(err, err.stack); // an error occurred
        else     console.log(data);           // successful response

        if (!err) {
            const jsonPayload = JSON.parse(data.payload);
            const lightDevices = jsonPayload.state.reported.lightDevices;
            if (lightDevices.length > 0) {
                jsonPayload.state.reported.lightDevices.forEach((item, index, array) => {
                    console.log("DEBUG", "lightDevices["+index+"]: "+JSON.stringify(item, null, 2));
                    const uriParts = item.uri.split("/");
                    const endpointId = uriParts[uriParts.length-1];
                    devices.push(generateDevice(endpointId, item.name, item.uri));
                });
            }
        }

        const response = {
            event: {
                header: {
                    namespace: "Alexa.Discovery",
                    name: "Discover.Response",
                    payloadVersion: "3",
                    messageId: generateMessageId()
                },
                payload: {
                    endpoints: devices
                }
            }
        };

        console.log("DEBUG", "Discovery Response: " + JSON.stringify(response, null, 2));
        callback(null, response);
    });
}

function getDeviceStateFromAwsIot(userAccessToken, endpointId, correlationToken, callback) {
    var deviceFound = false;
    const thingNameParam = {
        thingName: thingName
    };

    iotData.getThingShadow(thingNameParam, function(err, data) {
        console.log("DEBUG", "get thing shadow");
        if (err) console.log(err, err.stack); // an error occurred
        else     console.log(data);           // successful response

        if (!err) {
            const jsonPayload = JSON.parse(data.payload);
            const lightDevices = jsonPayload.state.reported.lightDevices;
            if (lightDevices.length > 0) {
                deviceFound = jsonPayload.state.reported.lightDevices.some((item, index, array) => {
                    console.log("DEBUG", "lightDevices["+index+"]: "+JSON.stringify(item, null, 2));
                    const uriParts = item.uri.split("/");
                    const endpointIdFromUri = uriParts[uriParts.length-1];
                    if (endpointId == endpointIdFromUri) {
                        const response = generateStateResponse(endpointId, correlationToken, item.powerOn, item.brightness);
                        console.log("DEBUG", "StateReport Response: " + JSON.stringify(response, null, 2));

                        callback(null, response);
                        return true;
                    }
                });
            }
        }
        if (!deviceFound) {
            const message = "EndpointId not found in device list";
            console.log("ERROR", message);
            callback(null, generateErrorResponse("ENDPOINT_UNREACHABLE", message, endpointId, correlationToken));
        }
    });
}

function setAwsIotDeviceState(userAccessToken, endpointId, correlationToken, powerState, brightness, delta, callback) {
    var deviceFound = false;
    const thingNameParam = {
        thingName: thingName
    };

    iotData.getThingShadow(thingNameParam, function(err, data) {
        console.log("DEBUG", "get thing shadow");
        if (err) console.log(err, err.stack); // an error occurred
        else     console.log(data);           // successful response

        if (!err) {
            const jsonPayload = JSON.parse(data.payload);
            const lightDevices = jsonPayload.state.reported.lightDevices;
            var response;
            if (lightDevices.length > 0) {
                jsonPayload.state.reported.lightDevices.forEach((item, index, array) => {
                    console.log("DEBUG", "lightDevices["+index+"]: "+JSON.stringify(item, null, 2));
                    const uriParts = item.uri.split("/");
                    const endpointIdFromUri = uriParts[uriParts.length-1];
                    if (endpointId == endpointIdFromUri) {
                        deviceFound = true;
                        // Set the desired state of the found light
                        if (powerState) {
                            const powerOn = (powerState == "ON") ? true : false;
                            lightDevices[index].powerOn = powerOn;
                            response = generateResponse("Alexa.PowerController", "powerState", powerState, endpointId, correlationToken);
                        }
                        else if (brightness) {
                            brightness = Math.min(brightness, 100);
                            brightness = Math.max(brightness, 0);
                            lightDevices[index].brightness = brightness;
                            response = generateResponse("Alexa.BrightnessController", "brightness", brightness, endpointId, correlationToken);
                        }
                        else if (delta) {
                            lightDevices[index].brightness += delta;
                            lightDevices[index].brightness = Math.min(lightDevices[index].brightness, 100);
                            lightDevices[index].brightness = Math.max(lightDevices[index].brightness, 0);
                            response = generateResponse("Alexa.BrightnessController", "brightness", lightDevices[index].brightness, endpointId, correlationToken);
                        } else {
                            console.log("DEBUG", "no state change requested? for endpointId " + endpointId);
                        }
                        console.log("DEBUG", "desired lightDevices["+index+"]: "+JSON.stringify(lightDevices[index], null, 2));
                    }
                });
            }

            if (deviceFound) {
                const desiredLightState = {"state":{"desired":{"lightDevices":lightDevices}}};
                const thingNameAndPayload = {
                        thingName: thingName,
                        payload: JSON.stringify(desiredLightState)
                };
                console.log("DEBUG", "payload: " + JSON.stringify(desiredLightState, null, 2));

                iotData.updateThingShadow(thingNameAndPayload, function(err, data) {
                    console.log("DEBUG", "update thing shadow");
                    if (err) console.log(err, err.stack); // an error occurred
                    else     console.log(data);           // successful response

                    if (!err) {
                        console.log("DEBUG", "Response: " + JSON.stringify(response, null, 2));
                        callback(null, response);

                    } else {
                        const message = "Unable to set device state";
                        console.log("ERROR", message);
                        callback(null, generateErrorResponse("INTERNAL_ERROR", message, endpointId, correlationToken));
                    }
                });

            } else {
                const message = "EndpointId not found in device list";
                console.log("ERROR", message);
                callback(null, generateErrorResponse("ENDPOINT_UNREACHABLE", message, endpointId, correlationToken));
            }
        }
    });
}

function handleDiscovery(request, callback) {
    console.log("DEBUG", "Discovery Request: " + JSON.stringify(request, null, 2));

    /**
     * Get the OAuth token from the request.
     */
    const userAccessToken = request.directive.payload.scope.token;
    console.log("DEBUG", "userAccessToken: " + userAccessToken);

    if (!userAccessToken || !isValidToken(userAccessToken)) {
        const message = "Invalid access token: " + userAccessToken;
        console.log("ERROR", message);
        callback(null, generateErrorResponse("INVALID_AUTHORIZATION_CREDENTIAL", message, "", ""));
        return;
    }

    /**
     * Retrieve list of devices from cloud based on token.
     */
    getDevicesFromAwsIot(userAccessToken, callback);
}

function handleReportState(request, callback) {
    console.log("DEBUG", "ReportState Request: " + JSON.stringify(request, null, 2));

    const correlationToken = request.directive.header.correlationToken;
    console.log("DEBUG", "correlationToken: " + correlationToken);

    const endpointId = request.directive.endpoint.endpointId;

    /**
     * Get the OAuth token from the request.
     */
    const userAccessToken = request.directive.endpoint.scope.token;
    console.log("DEBUG", "userAccessToken: " + userAccessToken);

    if (!userAccessToken || !isValidToken(userAccessToken)) {
        const message = "Invalid access token: " + userAccessToken;
        console.log("ERROR", message);
        callback(null, generateErrorResponse("INVALID_AUTHORIZATION_CREDENTIAL", message, endpointId, correlationToken));
        return;
    }

    if (!endpointId) {
        const message = "No endpointId provided in request";
        console.log("ERROR", message);
        callback(null, generateErrorResponse("NO_SUCH_ENDPOINT", message, endpointId, correlationToken));
        return;
    }

    /**
     * At this point the endpointId and accessToken are present in the request.
     */
    if (!isDeviceOnline(endpointId, userAccessToken)) {
        const message = "Target is offline";
        console.log("ERROR", message);
        callback(null, generateErrorResponse("ENDPOINT_UNREACHABLE", message, endpointId, correlationToken));
        return;
    }

    /**
     * Retrieve device state from cloud.
     */
    getDeviceStateFromAwsIot(userAccessToken, endpointId, correlationToken, callback);
}

function handleControl(request, callback) {
    console.log("DEBUG", "Control Request: " + JSON.stringify(request, null, 2));

    const correlationToken = request.directive.header.correlationToken;
    const endpointId = request.directive.endpoint.endpointId;

    /**
     * Get the access token from the request.
     */
    const userAccessToken = request.directive.endpoint.scope.token;
    console.log("DEBUG", "userAccessToken: " + userAccessToken);

    if (!userAccessToken || !isValidToken(userAccessToken)) {
        const message = "Invalid access token: " + userAccessToken;
        console.log("ERROR", message);
        callback(null, generateErrorResponse("INVALID_AUTHORIZATION_CREDENTIAL", message, endpointId, correlationToken));
        return;
    }

    if (!endpointId) {
        const message = "No endpointId provided in request";
        console.log("ERROR", message);
        callback(null, generateErrorResponse("NO_SUCH_ENDPOINT", message, endpointId, correlationToken));
        return;
    }

    /**
     * At this point the endpointId and accessToken are present in the request.
     */
    if (!isDeviceOnline(endpointId, userAccessToken)) {
        const message = "Target is offline";
        console.log("ERROR", message);
        callback(null, generateErrorResponse("ENDPOINT_UNREACHABLE", message, endpointId, correlationToken));
        return;
    }

    switch (request.directive.header.name) {
        case "TurnOn":
            setAwsIotDeviceState(userAccessToken, endpointId, correlationToken, "ON", null, null, callback);
            break;

        case "TurnOff":
            setAwsIotDeviceState(userAccessToken, endpointId, correlationToken, "OFF", null, null, callback);
            break;

        case "SetBrightness": {
            const brightness = request.directive.payload.brightness;
            if (!brightness) {
                const message = "No brightness provided in request";
                console.log("ERROR", message);
                callback(null, generateErrorResponse("INVALID_VALUE", message, endpointId, correlationToken));
                return;
            }
            setAwsIotDeviceState(userAccessToken, endpointId, correlationToken, null, brightness, null, callback);
            break;
        }

        case "AdjustBrightness": {
            const delta = request.directive.payload.brightnessDelta;
            if (!delta) {
                const message = "No brightness delta provided in request";
                console.log("ERROR", message);
                callback(null, generateErrorResponse("INVALID_VALUE", message, endpointId, correlationToken));
                return;
            }
            setAwsIotDeviceState(userAccessToken, endpointId, correlationToken, null, null, delta, callback);
            break;
        }

        default: {
            const message = "Directive not supported";
            console.log("ERROR", message);
            callback(null, generateErrorResponse("INVALID_DIRECTIVE", message, endpointId, correlationToken));
        }
    }
}

/**
 * Main entry point.
 * Incoming events from Alexa service through Smart Home API are all handled by this function.
 */
exports.handler = (request, context, callback) => {
    console.log("DEBUG", "Request: " + JSON.stringify(request, null, 2));

    switch (request.directive.header.namespace) {
        /**
         * The namespace of 'Alexa.Discovery' indicates a request is being made to the Lambda for
         * discovering all endpoints associated with the customer's endpoint cloud account.
         */
        case "Alexa.Discovery":
            handleDiscovery(request, callback);
            break;

        /**
         * The namespace of "Alexa.PowerController" and "Alexa.BrightnessController" indicates control request
         */
        case "Alexa.PowerController":
        case "Alexa.BrightnessController":
            handleControl(request, callback);
            break;

        /**
         * The namespace of "Alexa" with a name of "ReportState" indicates state request
         */
        case "Alexa":
            if (request.directive.header.name === "ReportState") {
                handleReportState(request, callback);

            } else {
                const message = "Directive not supported";
                console.log("ERROR", message);
                callback(null, generateErrorResponse("INVALID_DIRECTIVE", message, "", ""));
            }
            break;

        /**
         * Received an unexpected message
         */
        default: {
            const message = "Namespace not supported";
            console.log("ERROR", message);
            callback(null, generateErrorResponse("INVALID_DIRECTIVE", message, "", ""));
        }
    }
};
