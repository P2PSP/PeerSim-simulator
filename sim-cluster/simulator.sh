set -x
home="/home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster"
sleep_time=100s
counter=300
#upper_limit=400
buffer_size=32

cd ${home}/timing/
rm -rf ./*
cd ..
#while [ $counter -le $upper_limit ];
#do
##32 BITS
	echo "Experiment 1 for ${counter} peers, buffer 32"
	${home}/simulation.sh -c 0 -n ${counter} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/1/
	mkdir ${home}/timing/1/buffer-32bits
	mv ${home}/timing/peer* ${home}/timing/1/buffer-32bits
	#let counter=counter+10

	echo "Experiment 2 for ${counter} peers, buffer 32"
	${home}/simulation.sh -c 0 -n ${counter} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/2/
	mkdir ${home}/timing/2/buffer-32bits
	mv ${home}/timing/peer* ${home}/timing/2/buffer-32bits
	 #let counter=counter+10

	echo "Experiment 3 for ${counter} peers, buffer 32"
	${home}/simulation.sh -c 0 -n ${counter} -s 32 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/3/
	mkdir ${home}/timing/3/buffer-32bits
	mv ${home}/timing/peer* ${home}/timing/3/buffer-32bits
	#let counter=counter+10

#256 BITS
	echo "Experiment 1 for ${counter} peers, buffer 256"
	${home}/simulation.sh -c 0 -n ${counter} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/1/
	mkdir ${home}/timing/1/buffer-256bits
	mv ${home}/timing/peer* ${home}/timing/1/buffer-256bits
	#let counter=counter+10

	echo "Experiment 2 for ${counter} peers, buffer 256"
	${home}/simulation.sh -c 0 -n ${counter} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/2/
	mkdir ${home}/timing/2/buffer-256bits
	mv ${home}/timing/peer* ${home}/timing/2/buffer-256bits
	 #let counter=counter+10

	echo "Experiment 3 for ${counter} peers, buffer 256"
	${home}/simulation.sh -c 0 -n ${counter} -s 256 &
	sleep ${sleep_time}
	${home}/stop_simulation.sh
	#mkdir ${home}/mediciones_buffer/${counter}/
	mkdir ${home}/timing/3/
	mkdir ${home}/timing/3/buffer-256bits
	mv ${home}/timing/peer* ${home}/timing/3/buffer-256bits
	#let counter=counter+10

#done
set +x

