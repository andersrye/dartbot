#!/bin/bash
message=${1-hello}
port=${2-5000}
echo "Broadcasting \"$message\" on port $port"
echo $message | socat - UDP-DATAGRAM:255.255.255.255:$port,broadcast
