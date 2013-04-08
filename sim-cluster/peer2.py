#!/usr/bin/env python
# -*- coding: iso-8859-15 -*-
#
# peer.py
#

# {{{ Imports

import getopt
import sys
import socket
from blocking_socket import blocking_socket
from colors import Color
import struct
from time import gmtime, strftime
import argparse

# }}}

IP_ADDR = 0
PORT = 1

buffer_size = 32
cluster_port = 0 # OS default behavior will be used for port binding
player__port = 9999
source = "150.214.150.68:4552"
header_size = 20
block_size = 1024 # <- Ojo, valor recibido desde la fuente

# {{{ Args handing

parser = argparse.ArgumentParser(description='This is a peer node of a P2PSP cluster.')
parser.add_argument('--buffer_size=size of the video buffer in blocks'.format(buffer_size))
parser.add_argument('--cluster_port', help='port used to communicate with the cluster. (Default = {})'.format(cluster_port))
parser.add_argument('--player_port', help='port used to communicate with the player. (Default = {})'.format(player_port))
parser.add_argument('--source', help='endpoint of the source. (Default = {})'.format(source))

args = parser.parse_known_args()[0]
if args.block_size:
    buffer_size = int(args.buffer_size)
if args.player_port:
    player_port = int(args.player_port)
if args.cluster_port:
    cluster_port = int(args.cluster_port)
if args.source:
    source = args.source

# }}}

peer_list = []

# {{{ Wait for the player

player_listen_socket = blocking_socket(socket.AF_INET, socket.SOCK_STREAM)
player_listen_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
player_listen_socket.bind(("localhost", player_port))
player_listen_socket.listen(1)
print player_listen_socket.getsockname(), "Waiting for the player ...",
player_serve_socket, player = player_listen_socket.baccept()
player_listen_socket.setblocking(0)
print player_serve_socket.getsockname(), "accepted connection from", player

# }}}

# {{{ Connect to the source

if peer_port > 0:
    source_socket = socket.create_connection((source_name, source_port),1000,('',peer_port))
else:
    # Maybe this is redundant
    source_socket = blocking_socket(socket.AF_INET, socket.SOCK_STREAM)
    source_socket.connect((source_name, source_port))

print source_socket.getsockname(), "Connected to", source_socket.getpeername()

print source_socket.getsockname(), \
    "My IP address is" , source_socket.getsockname(), "->", \
    source_socket.getpeername()

# }}}

# {{{ Receive the video header from the source (and send it to the player)

print source_socket.getsockname(), "<- ", source_socket.getpeername(), "[Video header",
header_size = socket.ntohs(struct.unpack("H", source_socket.recv(struct.calcsize("H")))[0])
for i in xrange (header_size):
    block = source_socket.recv(block_size)
    player_serve_socket.sendall(block)
    print "\b.",
print "] done"

# }}}

# {{{ Receive a block and the list of peers from the source

number_of_peers = socket.ntohs(struct.unpack("H", source_socket.recv(struct.calcsize("H")))[0])
print source_socket.getsockname(), "<- Actual cluster size (without me) =", number_of_peers
print source_socket.getsockname(), "Retrieving the list of peers ..."
while number_of_peers > 0:
    payload = source_socket.recv(struct.calcsize("4sH"))
    peer_IPaddr, port = struct.unpack("4sH", payload)
    peer_IPaddr = socket.inet_ntoa(peer_IPaddr)
    port = socket.ntohs(port)
    peer = (peer_IPaddr, port)
    print source_socket.getsockname(), "<- peer", peer
    peer_list.append(peer)

    

    number_of_peers -= 1

print source_socket.getsockname(), "List of peers received"

# }}}

# {{{ Transform the peer-source TCP socket into a UDP socket

stream_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
stream_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
stream_socket.bind(('',source_socket.getsockname()[PORT]))

# }}}

# {{{ Create a working entry in the NAT, if neccesary

# This should create a working entry in the NAT if the peer is in a
# private network, and should alert to the rest of the peers of the
# cluster that a new peer is in it. If this peer in unreacheable and
# the super-peer has received one of these messages, the unreacheable
# peer should be removed by the super-peer and next, by the source.
payload = struct.pack("4sH", "aaaa", 0)
for p in peer_list:
    print "Sending an empty block to", p
    stream_socket.sendto(payload, p)
#for i in xrange(2):
#    stream_socket.sendto(payload, source_socket.getpeername())

# }}}

# {{{ Buffer creation

class Block_buffer_element:
    # {{{

    def block(self):
        return self[0]
    def number(self):
        return self[1]
    def empty(self):
        return self[2]

    # }}}

block_buffer = [Block_buffer_element() for i in xrange(buffer_size)]
for i in xrange(buffer_size):
    block_buffer[i].empty = True # Nothing useful inside

# }}}

counter = 0
lastpayload = None

def receive_and_feed_the_cluster():
    # {{{

    global counter
    global lastpayload
    
    try:
        payload, addr = stream_socket.recvfrom(struct.calcsize("H1024s"))
    except socket.timeout:
        sys.stderr.write("Lost connection to the source") 
        sys.exit(-1)

    print strftime("[%Y-%m-%d %H:%M:%S]", gmtime()), \
        str(sys.argv) + ": Received a block from", addr,"of length", len(payload)

    # Handle empty packets
    if len(payload) == struct.calcsize("H1024s"):

        number, block = struct.unpack("H1024s", payload)
        number = socket.ntohs(number)
        '''
        print source_socket.getsockname(),
        if block_buffer[number % buffer_size].requested:
            print Color.red + "<~" + Color.none,
        else:
            print Color.green + "<-" + Color.none, 
        print number, addr

        print source_socket.getsockname(),
        print Color.green + "<-" + Color.none,
        print number, addr
        '''
        #print "recive from ", addr, " number ", number

        if addr == source_socket.getpeername():        

            while((counter<len(peer_list))&(counter>0)):

                print "!!!!!!!!!!!!!!"

                peer=peer_list[counter]
                '''
                print source_socket.getsockname(), \
                    number, Color.green + "->" + Color.none, peer_list[counter], "(", counter+1, "/", len(peer_list),")"
                '''
                stream_socket.sendto(lastpayload, peer)
                peer_insolidarity[peer] += 1
                if peer_insolidarity[peer] > 64: # <- Important parameter!!
                    del peer_insolidarity[peer]
                    print Color.blue
                    print "Removing", peer
                    print Color.none

                    payload = struct.pack("4sH",
                                          socket.inet_aton(peer[IP_ADDR]),
                                          socket.htons(peer[PORT]))
                    stream_socket.sendto(payload, source_socket.getpeername())
                    peer_list.remove(peer)
                counter += 1

            counter=0
            lastpayload=payload
                    # Si este paquete se pierde, en principio no ocurre
                    # nada porque el o los super-peers van a hacer lo
                    # mismo con el nodo fuente y es prácticamente
                    # imposible que los mensajes que se envían desde los
                    # super-peers hacia el nodo fuente se pierdan (el
                    # ancho de banda entre ellos está garantizado).
        else:

            if addr not in peer_list:
                peer_list.append(addr)

            peer_insolidarity[addr] = 0

        #print ">>>>>>>>>>>>>>>> counter =", counter, "len(peer_list) =", len(peer_list)

        if(counter<len(peer_list)): 
            peer=peer_list[counter]

            '''
            print source_socket.getsockname(), \
                number, Color.green + "->" + Color.none, peer_list[counter], "(", counter+1, "/", len(peer_list),")"
            '''
            if(lastpayload!=None):
                stream_socket.sendto(lastpayload, peer)
            peer_insolidarity[peer] += 1 # Ojo, da un error en la
                                         # primera iteraci'on del
                                         # super-peer porque el peer
                                         # todav'ia no ha sido
                                         # incluido en el diccionario
            if peer_insolidarity[peer] > 64: # <- Important parameter!!
                del peer_insolidarity[peer]
                print Color.blue
                print "Removing", peer
                print Color.none

                payload = struct.pack("4sH",
                                      socket.inet_aton(peer[IP_ADDR]),
                                      socket.htons(peer[PORT]))
                stream_socket.sendto(payload, source_socket.getpeername())
                peer_list.remove(peer)
            counter += 1

        block_buffer[number % buffer_size].block = block
        block_buffer[number % buffer_size].number = number
        block_buffer[number % buffer_size].empty = False

        return number

    else:

        # An empty packet has been received

        if addr not in peer_list:
            peer_list.append(addr)
            peer_insolidarity[addr] = 0

        return 0

    # }}}

# {{{ Buffering

print
print "Buffering ..."
print

block_to_play = receive_and_feed_the_cluster()
for i in xrange(buffer_size/2):
    print strftime("[%Y-%m-%d %H:%M:%S]", gmtime()), \
        str(sys.argv) + ": Received block " + str(i) + "/" + str(buffer_size/2)
    receive_and_feed_the_cluster()

# Now, reset the solidarity of the peers
for p in peer_list:
    peer_insolidarity[p] = 0

print "... buffering done"

# }}}

def send_a_block_to_the_player():
    # {{{

    global block_to_play

    # Only if the block has something useful inside ...
    if block_buffer[(block_to_play % buffer_size)].empty == False:
        '''
        print player_serve_socket.getsockname(), \
            block_buffer[block_to_play % buffer_size].number, \
            Color.blue + "=>" + Color.none, \
            player_serve_socket.getpeername()
        '''
        try:
            player_serve_socket.sendall(block_buffer[block_to_play % buffer_size].block)
        except socket.error:
            sys.stderr.write("Conection closed by the player")
            sys.exit(-1)
        
        # buffer[block_to_play.number].empty = True
        block_buffer[block_to_play % buffer_size].empty = True
    #else:
       # print ("------------------------- missing block ---------------------")
    # Increment the block_to_play
    block_to_play = (block_to_play + 1) % 65536

    # }}}

while True:
    send_a_block_to_the_player()
    receive_and_feed_the_cluster()
