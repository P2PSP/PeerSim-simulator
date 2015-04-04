# P2PSP Simulation Project

This is the PeerSim simulation branch for P2PSP. Its purpose is to simulate practical conditions with large sets of clients in order to obtain more knowledge about its behaviour.

To run this simulations you need to download the PeerSim simulator from [PeerSim download page](http://sourceforge.net/projects/peersim/).

You can set up your IDE (i.e. Eclipse) to work with PeerSim project as it described [here](http://miromannino.com/integrating-peersim-with-eclipse/).

Configuration file is located at `config/config.txt`.

## Running simulation

1. Create a directory:
```shell
$ mkdir p2psp-peersim && cd p2psp-peersim
```
2. Clone a repository:
```shell
$ git clone git@github.com:P2PSP/sim.git
```
3. Download PeerSim simulator engine (and unzip it):
```shell
$ wget downloads.sourceforge.net/project/peersim/peersim-1.0.5.zip && unzip peersim-1.0.5.zip
```
4. Compile source files of P2PSP protocol:
```shell
$ javac -cp ./peersim-1.0.5/peersim-1.0.5.jar:./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar ./sim/src/*.java
```
5. Run simulation:
```shell
$ java -cp ./:./peersim-1.0.5/peersim-1.0.5.jar:./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar peersim.Simulator ./sim/config/config.txt 
```