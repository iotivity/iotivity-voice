<!---
  ~ //******************************************************************
  ~ //
  ~ // Copyright 2107 Intel Corporation All Rights Reserved.
  ~ //
  ~ //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  ~ //
  ~ // Licensed under the Apache License, Version 2.0 (the "License");
  ~ // you may not use this file except in compliance with the License.
  ~ // You may obtain a copy of the License at
  ~ //
  ~ //      http://www.apache.org/licenses/LICENSE-2.0
  ~ //
  ~ // Unless required by applicable law or agreed to in writing, software
  ~ // distributed under the License is distributed on an "AS IS" BASIS,
  ~ // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ // See the License for the specific language governing permissions and
  ~ // limitations under the License.
  ~ //
  ~ //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  --->

## Install required build libraries

You will need to build IoTivity prior to use.

    $ scons TARGET_TRANSPORT=IP SECURED=0 BUILD_JAVA=ON

Once built, the build.sh and run.sh scripts expect iotivity.jar to be copied to ./lib directory.

    $ cp <iotivity>/out/linux/x86_64/release/java/iotivity.jar lib/.


Additionally, these jar files are also expected to be in the ./lib directory:


[aws-iot-device-sdk-java-1.1.1.jar](https://s3.amazonaws.com/aws-iot-device-sdk-java/aws-iot-device-sdk-java-LATEST.zip)

[org.eclipse.paho.client.mqttv3-1.2.0.jar](https://www.eclipse.org/paho/)

[jackson-core-2.9.0.jar](https://jar-download.com/?search_box=jackson-core-2.9.0)

[jackson-databind-2.9.0.jar](https://jar-download.com/?search_box=jackson-databind-2.9.0)

[jackson-annotations-2.9.0.jar](https://jar-download.com/?search_box=jackson-annotations-2.9.0)

## Build

    $ ./build.sh

## Run

    $ ./run.sh 
