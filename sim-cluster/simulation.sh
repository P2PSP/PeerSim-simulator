#!/bin/bash

set -x

# Simulates flash-crowd peer churn.

#number_of_blocks=100
number_of_peers=2
churn_scale=0
buffer_size=32
block_size=1024

usage() {
    echo $0
    echo "Simulates flash-crowd peer churn."
    echo "Parameters:"
#    echo "  [-b (number of blocks, $number_of_blocks by default)]"
    echo "  [-n (number of peers, $number_of_peers by default)]"
    echo "  [-c (churn scale, $churn_scale by default, meaning no churn)]"
    echo "  [-s (buffer size in blocks, $buffer_size by default)]"
    echo " [-l (block size in bytes, $block_size by default)]"	
    echo "  [-? (help)]"
}

echo $0: parsing: $@

while getopts "b:n:c:s:l:?" opt; do
    case ${opt} in
	b)
	    number_of_blocks="${OPTARG}"
	    ;;
	n)
	    number_of_peers="${OPTARG}"
	    ;;
	c)
	    churn_scale="${OPTARG}"
	    ;;
	s)
	    buffer_size="${OPTARG}"
	    ;;
        l)
	    block_size="${OPTARG}"
	    ;;
	?)
	    usage
	    exit 0
	    ;;
	\?)
	    echo "Invalid option: -${OPTARG}" >&2
	    usage
	    exit 1
	    ;;
	:)
	    echo "Option -${OPTARG} requires an argument." >&2
	    usage
	    exit 1
	    ;;
    esac
done

echo "block_size is ${block_size}"

#clear previous output files
rm  /home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster/output/*
rm  /home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster/timing/*

#start the splitter
xterm -l -lf ./output/salida_splitter.txt -e "./splitter.py --source_hostname=localhost --logging_level=INFO --buffer_size=$buffer_size --block_size=$block_size"&

sleep 1

#start the gatherer
xterm -l -lf ./output/salida_gatherer.txt -e "./gatherer.py --splitter_hostname=localhost --source_hostname=localhost --logging_level=INFO --buffer_size=$buffer_size --block_size=$block_size" &

sleep 1s

#start the player
vlc http://localhost:9999 &

sleep 5s
#x(){
COUNTER=0
while [ $COUNTER -lt $number_of_peers ];
do
    ./peer-h.py --splitter_hostname=localhost --source_hostname=localhost --no_player --logging_level=DEBUG --logging_file=./output/peer-${COUNTER}  --churn=${churn_scale} --buffer_size=${buffer_size} --block_size=$block_size&
    let COUNTER=COUNTER+1 
done
#}
set +x
