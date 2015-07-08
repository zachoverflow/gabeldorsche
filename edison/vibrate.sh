#! /bin/sh

GPIO=/sys/class/gpio/gpio

echo $1 > ${GPIO}12/value
echo $2 > ${GPIO}13/value
echo $3 > ${GPIO}182/value
echo $4 > ${GPIO}183/value
