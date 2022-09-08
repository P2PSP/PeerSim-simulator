# Bitcoin transaction relay simulator

This project helps to measure different trade-offs in transaction relay protocols for Bitcoin.
It was primarily designed to compare configurations of Erlay-like protocols, but it also helps just
explore how flooding works.

The simulator assumes knowledge of the existing Bitcoin p2p stack.

It currently omits to send GETDATA/TX messages, because it is not necessary for the current
case, but can be easily expanded for that logic (as well as more advanced peer selection, block
propagation research, etc.).

Beware, research code.

## Organization

This project consists of several main classes:
1. Peer represents a normal Bitcoin node.
2. Peer initializer spawns and configures Bitcoin nodes.
3. Source represents a special node from which transactions initially propagate to random nodes.
4. Source Initializer spawns and configures source nodes.
5. Helpers contain custom message types to send between nodes.
6. InvObserver is a class to collect results at the end of the experiment.

## HOWTO

The configuration file is located at `config/config.txt`. In this file, you can specify network size,
connectivity, and other protocol-specific constants.

Also, you will need JDK for this.

1. Create a directory:

    ```shell
    mkdir p2p-simulations && cd p2p-simulations
    ```

2. Clone a repository:

    ```shell
    git clone git@github.com:naumenkogs/txrelaysim.git
    ```

3. Download PeerSim simulator engine (and unzip it):

    ```shell
    wget downloads.sourceforge.net/project/peersim/peersim-1.0.5.zip && unzip peersim-1.0.5.zip
    ```

4. Compile source files:

    ```shell
    javac -cp ./peersim-1.0.5/peersim-1.0.5.jar:./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar ./txrelaysim/src/*.java ./txrelaysim/src/helpers/*.java
    ```

5. Run simulation:

    ```shell
    java -cp ./:./peersim-1.0.5/peersim-1.0.5.jar:./peersim-1.0.5/jep-2.3.0.jar:./peersim-1.0.5/djep-1.0.0.jar peersim.Simulator ./txrelaysim/config/config.txt
    ```

## Result interpretation

We usually receive something like this at the end of the run:
```
1.7822426377729141 extra inv per tx on average.
2.155010635147142 shortInvs per tx on average.
23.500275013750688 success recons on average.
0.08350417520876044 failed recons on average.
Avg max latency: 7348.884615384615
```

For every transaction, no matter which protocol is used, the cost is always at least `INV + GETDATA + TX`.
This data demonstrates extra cost: `+ 1.78 INV + 2.15 * SHORT_INV`, where `SHORT_INV = INV / 4`.

`Avg max latency` represents the time it takes for a transaction to reach 95% of nodes.

## Results

Some results generated from the output of this simulator you can find in the Results folder.

## Scalability

On my 16" MacBook Pro 2019 it takes no more than a couple of minutes to simulate transaction relay across 30,000 nodes.
If you increase connectivity (more than 8) or the number of nodes, you might run out of memory.

For a more large-scale experiment, I may suggest using a machine with more RAM.
To make it faster, you probably want a faster CPU.
