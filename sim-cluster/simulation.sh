#!/bin/bash

set -x

# Simulates flash-crowd peer churn.

#number_of_blocks=100
number_of_peers=2
churn_scale=0

usage() {
    echo $0
    echo "Simulates flash-crowd peer churn."
    echo "Parameters:"
#    echo "  [-b (number of blocks, $number_of_blocks by default)]"
    echo "  [-n (number of peers, $number_of_peers by default)]"
    echo "  [-c (churn scale, $churn_scale by default, meaning no churn)]"
    echo "  [-? (help)]"
}

echo $0: parsing: $@

while getopts "b:n:c:?" opt; do
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

#clear previous output files
rm  /home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster/output/*
rm  /home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster/timing/*

#xterm -e "./splitter.py --block_size=$block_size --channel=$source_channel --source_hostname=$source_hostname --source_port=$source_port --listening_port=$splitter_port" &

#start the splitter
xterm -l -lf ./output/salida_splitter.txt -e "./splitter.py --source_hostname=localhost" &

sleep 1

#start the gatherer
#xterm -e "./gatherer.py --buffer_size=$buffer_size --listening_port=$[splitter_port+1] --channel=$source_channel --source_hostname=$source_hostname --source_port=$source_port --splitter_hostname=$splitter_hostname --splitter_port=$splitter_port" &
xterm -l -lf ./output/salida_gatherer.txt -e "./gatherer.py --splitter_hostname=localhost --source_hostname=localhost" &

sleep 1

#start the player
vlc http://localhost:9999 &

sleep 10
#x(){
COUNTER=0
while [ $COUNTER -lt $number_of_peers ];
do
#    ./peer.py --splitter_hostname=localhost --source_hostname=localhost --no_player --logging_level=DEBUG --logging_file=./output/peer-${COUNTER}  --churn=0 &
    ./peer.py --splitter_hostname=localhost --source_hostname=localhost --no_player --logging_level=DEBUG --logging_file=./output/peer-${COUNTER}  --churn=${churn_scale} &
    let COUNTER=COUNTER+1 
#    sleep 0.5
done
#}
set +x
