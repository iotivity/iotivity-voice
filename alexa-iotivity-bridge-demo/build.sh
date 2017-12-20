#!/bin/bash

#find ./out -type f -name '*.class' -delete
rm -rf ./out
mkdir out
javac @options.txt  @sourcefiles.txt

#jar -cfv AlexaIotivityBridgeDemo.jar -C out/ .
jar -cf AlexaIotivityBridgeDemo.jar -C out/ .

