#!/bin/bash

set -x

# Simulates flash-crowd peer churn.

#number_of_blocks=100
number_of_peers=2

usage() {
    echo $0
    echo "Simulates flash-crowd peer churn."
    echo "Parameters:"
#    echo "  [-b (number of blocks, $number_of_blocks by default)]"
    echo "  [-n (number of peers, $number_of_peers by default)]"
    echo "  [-? (help)]"
}

echo $0: parsing: $@

while getopts "b:n:?" opt; do
    case ${opt} in
	b)
	    number_of_blocks="${OPTARG}"
	    ;;
	n)
	    number_of_peers="${OPTARG}"
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

COUNTER=0
while [ $COUNTER -lt $number_of_peers ];
do
    #./peer.py --buffer_size=$buffer_size --listening_port=$[splitter_port+1] --channel="$source_channel" --source_hostname="$source_hostname" --source_port=$source_port --splitter_hostname="$splitter_hostname" --splitter_port=$splitter_port --no_player -number_of_blocks=100 &
    #./peer.py --splitter_hostname=localhost --no_player --number_of_blocks=$number_of_blocks &
    #./peer.py --splitter_hostname=localhost --no_player --logging_level=DEBUG > ./output/peer-${COUNTER} &
    ./peer.py --splitter_hostname=localhost --no_player --logging_level=DEBUG --logging_file=./output/peer-${COUNTER}  --churn=0 &
    let COUNTER=COUNTER+1 

done

set +x
