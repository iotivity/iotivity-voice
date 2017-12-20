# -cp assumes iotivity.jar has been copied into ./lib/

java -Djava.library.path=/home/larrys/work/iotivity-1.3-rel/iotivity/out/linux/x86_64/release -cp AlexaIotivityBridgeDemo.jar:./lib/iotivity.jar org.iotivity.base.examples.IotivityScanner $1