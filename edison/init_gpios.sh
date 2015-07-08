#! /bin/sh

GPIO=/sys/class/gpio

echo 12 > $GPIO/export
echo 13 > $GPIO/export
echo 182 > $GPIO/export
echo 183 > $GPIO/export

echo out > $GPIO/gpio12/direction
echo out > $GPIO/gpio13/direction
echo out > $GPIO/gpio182/direction
echo out > $GPIO/gpio183/direction

rfkill unblock bluetooth
rfkill block wifi
