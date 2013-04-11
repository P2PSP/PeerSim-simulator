#!/bin/bash

# Lauch a splitter, a gatherer and a player.

block_size=1024
buffer_size=32 # blocks
source_channel=134.ogg
source_hostname=localhost
source_port=4551
splitter_hostname=localhost
splitter_port=4552
gatherer_port=9999

usage() {
    echo $0
    echo "Launches a splitter, a gatherer and a player."
    echo "Parameters:"
    echo "  [-b (block size, \"$block_size\" by default)]"
    echo "  [-u (buffer size, \"$buffer_size\" by default)]"
    echo "  [-c (source channel, \"$source_channel\" by default)]"
    echo "  [-a (source hostname, $source_hostname by default)]"
    echo "  [-p (source port, $source_port by default)]"
    echo "  [-n (number of peers, $number_of_peers by default)]"
    echo "  [-l (splitter port, $splitter_port by default)]"
    echo "  [-s (splitter hostname, $spltter_hostname by default)]"
    echo "  [-v (video filename, \"$video\" by default)]"
    echo "  [-? (help)]"
}

echo $0: parsing: $@

while getopts "b:u:c:w:a:p:l:s:v:?" opt; do
    case ${opt} in
	b)
	    block_size="${OPTARG}"
	    ;;
	u)
	    buffer_size="${OPTARG}"
	    ;;
	c)
	    source_channel="${OPTARG}"
	    ;;
	a)
	    source_hostname="${OPTARG}"
	    ;;
	p)
	    source_port="${OPTARG}"
	    ;;
	l)
	    splitter_port="${OPTARG}"
	    ;;
	s)
	    splitter_hostname="${OPTARG}"
	    ;;
	v)
	    video="${OPTARG}"
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

#xterm -e "./splitter.py --block_size=$block_size --channel=$source_channel --source_hostname=$source_hostname --source_port=$source_port --listening_port=$splitter_port" &

#start the splitter
xterm -l -lf ./output/salida_splitter.txt -e "./splitter.py --source_hostname=localhost" &

sleep 1

#start the gatherer
#xterm -e "./gatherer.py --buffer_size=$buffer_size --listening_port=$[splitter_port+1] --channel=$source_channel --source_hostname=$source_hostname --source_port=$source_port --splitter_hostname=$splitter_hostname --splitter_port=$splitter_port" &
xterm -l -lf ./output/salida_gatherer.txt -e "./gatherer.py --splitter_hostname=localhost" &

sleep 1

#start the player
vlc http://localhost:9999 &

#start all peers
