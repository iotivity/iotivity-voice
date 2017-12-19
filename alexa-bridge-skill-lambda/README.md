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

## Install required node.js modules

You will need to run npm install to create a node_modules folder.

    $ npm install aws-sdk alexa-sdk

Create a zip file with index.js and node_modules.

    $ zip -r aws-lambda-upload.zip index.js node_modules/

aws-lambda-upload.zip will be the zip file for upload.

Subsequent updates to the zip file can be done with

    $ zip aws-lambda-upload.zip index.js


