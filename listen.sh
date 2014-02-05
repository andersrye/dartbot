#!/bin/bash
port=${1-5000}
#echo "Listening on for datagrams on port $port"
while true; do nc -l -u -w 0 $port; done;
