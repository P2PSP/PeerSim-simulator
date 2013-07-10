set -x
home="/home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster"
strategy="bloque-exclusivo-nuevos-siguiente-block768"
sleep_time=100s
sleep_time_oggfwd=30s
num_peers_array=(10 50 100 300)
#upper_limit=400

cd ${home}/timing/
rm -rf ./*
cd ..

pkill oggfwd

for num_peers in ${num_peers_array[*]}
do

##32 BITS
	echo "Experiment 1 for ${num_peers} peers, buffer 32"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/1/${strategy}/buffer-32bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/1/${strategy}/buffer-32bits/${num_peers}
	pkill oggfwd

	echo "Experiment 2 for ${num_peers} peers, buffer 32"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/2/${strategy}/buffer-32bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/2/${strategy}/buffer-32bits/${num_peers}
	pkill oggfwd

	echo "Experiment 3 for ${num_peers} peers, buffer 32"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/3/${strategy}/buffer-32bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/3/${strategy}/buffer-32bits/${num_peers}
	pkill oggfwd

#256 BITS
	echo "Experiment 1 for ${num_peers} peers, buffer 256"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/1/${strategy}/buffer-256bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/1/${strategy}/buffer-256bits/${num_peers}
	pkill oggfwd

	echo "Experiment 2 for ${num_peers} peers, buffer 256"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/2/${strategy}/buffer-256bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/2/${strategy}/buffer-256bits/${num_peers}
	pkill oggfwd

	echo "Experiment 3 for ${num_peers} peers, buffer 256"
	./run_oggfwd.sh &
	sleep ${sleep_time_oggfwd}
	${home}/simulation.sh -c 0 -n ${num_peers} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	mkdir -p ${home}/timing/3/${strategy}/buffer-256bits/${num_peers}
	mv ${home}/timing/peer* ${home}/timing/3/${strategy}/buffer-256bits/${num_peers}
	pkill oggfwd

done
set +x

