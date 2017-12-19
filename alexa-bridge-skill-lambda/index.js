"use strict";
var config = {};

var AWS = require("aws-sdk");
var Alexa = require("alexa-sdk");

// replace with endpoint from AWS IoT Device
var iotData = new AWS.IotData({endpoint:"a2a16j9xf0mmy6.iot.us-east-1.amazonaws.com", region:"us-east-1"});
// replace with Thing Name for AWS IoT Device
var thingName = "larryslinuxbox";

var APP_ID = "amzn1.ask.skill.04c3ebae-673e-48ed-8892-bb58feb3eb54";
var SKILL_NAME = "AlexaIotivityBridgeSkill";

exports.handler = function(event, context, callback) {
    console.log("Received event:", JSON.stringify(event, null, 2));
    var alexa = Alexa.handler(event, context);
    alexa.appId = APP_ID;
    alexa.registerHandlers(handlers);
    alexa.execute();
};

var handlers = {
    "LaunchRequest": function() {
        this.emit(":tell", "Bridge Operational");
    },

    "ListLightsIntent": function() {
        awsIot.getLights(this);
    },

    "AllLightsStateIntent": function() {
        var itemSlot = this.event.request.intent.slots.lightState;
        var requestedLightState;
        if (itemSlot && itemSlot.value) {
            var itemSlotValue = itemSlot.value.toLowerCase();
            console.log("itemSlotValue: " + itemSlotValue);
            if (itemSlotValue == "on" || itemSlotValue == "off") {
                requestedLightState = itemSlotValue;
        }
    }
    console.log("requestedLightState: " + requestedLightState);

    if (!requestedLightState) {
        this.emit(":tell", "Sorry, I didn't get that");
        } else {
            var speechOutput = "All lights " + requestedLightState;
            awsIot.allLightsStateChange(this, requestedLightState, speechOutput);
        }
    },

    "LightBrightnessIntent": function() {
        var lightName = this.event.request.intent.slots.lightName.value;
        var lightBrightness = this.event.request.intent.slots.lightBrightness.value;
        console.log("lightName: " + lightName);
        console.log("lightBrightness: " + lightBrightness);

        if (!lightName || !lightBrightness) {
            this.emit(":tell", "Sorry, I didn't get that");
        } else {
            awsIot.updateLight(this, postGetLights.updateBrightness, lightName, lightBrightness);
        }
    },

    "LightStateIntent": function() {
        var lightName = this.event.request.intent.slots.lightName.value;
        var lightState = this.event.request.intent.slots.lightState.value;
        console.log("lightName: " + lightName);
        console.log("lightState: " + lightState);

        if (!lightName || !lightState) {
            this.emit(":tell", "Sorry, I didn't get that");
        } else {
            awsIot.updateLight(this, postGetLights.updateOnOff, lightName, lightState);
        }
    },

    "LightRenameIntent": function() {
        var lightName = this.event.request.intent.slots.lightName.value;
        var newLightName = this.event.request.intent.slots.newLightName.value;
        console.log("lightName: " + lightName);
        console.log("newLightName: " + newLightName);

        if (!lightName || !newLightName) {
            this.emit(":tell", "Sorry, I didn't get that");
        } else {
            awsIot.updateLight(this, postGetLights.updateName, lightName, newLightName);
        }
    },

    "AMAZON.HelpIntent": function() {
        this.attributes["speechOutput"] = "You can ask questions such as, 'List Lights', or you can say 'exit'... " +
        "Now, what can I help you with?";
        this.attributes["repromptSpeech"] = "You can say things like, 'List Lights', or you can say 'exit'... " +
        "Now, what can I help you with?";
        this.emit(":ask", this.attributes["speechOutput"], this.attributes["repromptSpeech"]);
    },

    "AMAZON.RepeatIntent": function() {
        this.emit(":ask", this.attributes["speechOutput"], this.attributes["repromptSpeech"]);
    },

    "AMAZON.StopIntent": function() {
        this.emit("SessionEndedRequest");
    },

    "AMAZON.CancelIntent": function() {
        this.emit("SessionEndedRequest");
    },

    "SessionEndedRequest":function() {
        this.emit(":tell", "Goodbye!");
    }
};

var awsIot = {
    postSendMessage: function(thisOfCaller, speechOutput) {
        console.log("post send message: " + speechOutput);
        thisOfCaller.emit(":tell", speechOutput);
    },

    allLightsStateChange: function(thisOfCaller, requestedStateForEachLight, speechOutput) {
        // update the state for each light
        var thingNameParam = {
                thingName: thingName
        };

        iotData.getThingShadow(thingNameParam, function(err, data) {
            console.log("get thing shadow (in sendAllOnOff)")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput = "Sorry, no lights found."
                awsIot.postSendMessage(thisOfCaller, speechOutput);
            }
            else {
                var jsonPayload = JSON.parse(data.payload);
                var lightDevices = jsonPayload.state.reported.lightDevices;
                if (lightDevices.length > 0) {
                    lightDevices.forEach((item, index, array) => {
                        console.log("lightDevices["+index+"]: "+JSON.stringify(item));
                    });
                } else {
                    console.log("no lights found");
                    speechOutput = "Sorry, no lights found.";
                }

                postGetLights.updateOnOff(thisOfCaller, lightDevices, null, requestedStateForEachLight, speechOutput);
            }
        });
    },

    getLights: function(thisOfCaller) {
        var speechOutput = "";
        var thingNameParam = {
                thingName: thingName
        };

        iotData.getThingShadow(thingNameParam, function(err, data) {
            console.log("get thing shadow")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput += " - Error getting device list: " + err + " ";
                speechOutput += thingNameParam.thingName;
            } else {
                var jsonPayload = JSON.parse(data.payload);
                var lightDevices = jsonPayload.state.reported.lightDevices;
                if (lightDevices.length > 0) {
                    jsonPayload.state.reported.lightDevices.forEach((item, index, array) => {
                        console.log("lightDevices["+index+"]: "+JSON.stringify(item));
                        if (index > 0) {
                            speechOutput += ", ";
                        }
                        speechOutput += item.name + " is " + (item.powerOn ? "on" : "off");
                    });
                } else {
                    speechOutput = "Sorry, no lights found."
                }
            }

            awsIot.postSendMessage(thisOfCaller, speechOutput);
        });
    },

    updateLight: function(thisOfCaller, postGetLights, lightName, lightParam) {
        var speechOutput = "";
        var thingNameParam = {
                thingName: thingName
        };

        iotData.getThingShadow(thingNameParam, function(err, data) {
            console.log("get thing shadow (in updateLights)")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput = "Sorry, no lights found."
                awsIot.postSendMessage(thisOfCaller, speechOutput);
            }
            else {
                var foundLightName = false;
                var jsonPayload = JSON.parse(data.payload);
                var lightDevices = jsonPayload.state.reported.lightDevices;
                if (lightDevices.length > 0) {
                    lightDevices.forEach((item, index, array) => {
                        console.log("lightDevices["+index+"]: "+JSON.stringify(item));
                        if (item.name.toLowerCase() == lightName.toLowerCase()) {
                            console.log("light found -- lightDevices["+index+"]: "+JSON.stringify(item));
                            foundLightName = true;
                        }
                    });
                } else {
                    console.log("no lights found");
                }
                if (foundLightName) {
                    postGetLights(thisOfCaller, lightDevices, lightName, lightParam, speechOutput);
                } else {
                    speechOutput = "Sorry, I can't find " + lightName;
                    awsIot.postSendMessage(thisOfCaller, speechOutput);
                }
            }
        });
    }
};

var postGetLights = {
    updateBrightness: function(thisOfCaller, lightDevices, lightName, lightParam, speechOutput) {
        // Set the desired brightness of the light
        var newLightBrightness;
        if (lightParam < 0) {
            newLightBrightness = 0;
        } else if (lightParam > 100) {
            newLightBrightness = 100;
        } else {
            newLightBrightness = lightParam * 1; // make it a number
        }

        lightDevices.forEach((item, index, array) => {
            console.log("lightDevices["+index+"]: "+JSON.stringify(item));
            if (item.name.toLowerCase() == lightName.toLowerCase()) {
                lightDevices[index].brightness = newLightBrightness;
            }
            console.log("desired lightDevices["+index+"]: "+JSON.stringify(lightDevices[index]));
        });
        var desiredLightState =
        {"state":{"desired":{"device":thingName,"lightDevices":lightDevices}}};

        var thingNameAndPayload = {
                thingName: thingName,
                payload: JSON.stringify(desiredLightState)
        };
        console.log("payload:", JSON.stringify(desiredLightState, null, 2));

        iotData.updateThingShadow(thingNameAndPayload, function(err, data) {
            console.log("updateBrightness update thing shadow")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput += " - Error updating light: " + err + " ";
                speechOutput += thingNameAndPayload.thingName + " ";
                speechOutput += thingNameAndPayload.payload;
            } else {
                speechOutput = lightName + " brightness " + newLightBrightness;
            }
            awsIot.postSendMessage(thisOfCaller, speechOutput);
        });
    },

    updateOnOff: function(thisOfCaller, lightDevices, lightName, lightParam, speechOutput) {
        // Set the desired state of the light
        // lightName of null means all lights
        console.log("lightName = "+lightName+", !lightName="+(!lightName));
        var newLightState = (lightParam.toLowerCase() == "on");
        lightDevices.forEach((item, index, array) => {
            console.log("lightDevices["+index+"]: "+JSON.stringify(item));
            if ((! lightName) || (item.name.toLowerCase() == lightName.toLowerCase())) {
                lightDevices[index].powerOn = newLightState;
            }
            console.log("desired lightDevices["+index+"]: "+JSON.stringify(lightDevices[index]));
        });
        var desiredLightState =
        {"state":{"desired":{"device":thingName,"lightDevices":lightDevices}}};

        var thingNameAndPayload = {
                thingName: thingName,
                payload: JSON.stringify(desiredLightState)
        };
        console.log("payload:", JSON.stringify(desiredLightState, null, 2));

        iotData.updateThingShadow(thingNameAndPayload, function(err, data) {
            console.log("updateOnOff update thing shadow")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput += " - Error updating light: " + err + " ";
                speechOutput += thingNameAndPayload.thingName + " ";
                speechOutput += thingNameAndPayload.payload;
            } else {
                if (lightName) {
                    speechOutput = lightName + " " + (newLightState ? "on" : "off");
                }
            }
            awsIot.postSendMessage(thisOfCaller, speechOutput);
        });
    },

    updateName: function(thisOfCaller, lightDevices, lightName, lightParam, speechOutput) {
        // Set the desired name of the light
        var newLightName = lightParam;

        lightDevices.forEach((item, index, array) => {
            console.log("lightDevices["+index+"]: "+JSON.stringify(item));
            if (item.name.toLowerCase() == lightName.toLowerCase()) {
                lightDevices[index].name = newLightName;
            }
            console.log("desired lightDevices["+index+"]: "+JSON.stringify(lightDevices[index]));
        });

        // sort desired light devices by name (ignoring case)
        lightDevices.sort((lhs, rhs) => {
            var lhsName = lhs.name.toLowerCase();
            var rhsName = rhs.name.toLowerCase();

            if (lhsName < rhsName) {
                return -1;
            }
            if (lhsName > rhsName) {
                return 1;
            }
            // names must be equal
            return 0;
        });
        lightDevices.forEach((item, index, array) => {
            console.log("sorted desired lightDevices["+index+"]: "+JSON.stringify(item));
        });

        var desiredLightState =
        {"state":{"desired":{"device":thingName,"lightDevices":lightDevices}}};

        var thingNameAndPayload = {
                thingName: thingName,
                payload: JSON.stringify(desiredLightState)
        };
        console.log("payload:", JSON.stringify(desiredLightState, null, 2));

        iotData.updateThingShadow(thingNameAndPayload, function(err, data) {
            console.log("updateName update thing shadow")
            if (err) console.log(err, err.stack); // an error occurred
            else     console.log(data);           // successful response

            if (err) {
                speechOutput += " - Error updating light: " + err + " ";
                speechOutput += thingNameAndPayload.thingName + " ";
                speechOutput += thingNameAndPayload.payload;
            } else {
                speechOutput = lightName + " renamed as " + newLightName;
            }
            awsIot.postSendMessage(thisOfCaller, speechOutput);
        });
    }
};
