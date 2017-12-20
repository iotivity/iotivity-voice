#!/bin/bash

#find ./out -type f -name '*.class' -delete
rm -rf ./out
mkdir out
javac @options.txt  @sourcefiles.txt

#jar -cfv OcfLightDevice.jar -C out/ .
#jar -ufv OcfLightDevice.jar res/
jar -cf OcfLightDevice.jar -C out/ .
jar -uf OcfLightDevice.jar res/

