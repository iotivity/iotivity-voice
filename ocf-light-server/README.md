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

## Build

    $ ./build.sh

## Run Command line parameters

You can optionally set the name of the light and its inital on/off state and brightness.

    $ ./run.sh [ lightName [ on | off [ 0 - 100 ] ] ]

For example:

    $ ./run.sh "My Favorite Light" on 75

If no parameters are given, a random light name is generated with power on and brightness 100.

Any OCF client should be able to interact with the light.  For example, the Client Controller of the IoTivity Simulator.  See https://wiki.iotivity.org/iotivity_tool_guide 


