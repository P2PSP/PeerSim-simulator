#!/usr/bin/env python
# -*- coding: iso-8859-15 -*-
#
# drain.py
#

# {{{ Imports

import sys
import socket
from colors import Color
import struct
from time import gmtime, strftime
import argparse
import threading
from threading import Lock
import blocking_socket

# }}}

IP_ADDR = 0
PORT = 1

buffer_size = 32
#cluster_port = 0 # OS default behavior will be used for port binding
clients_port = 9999
server_IP = '150.214.150.68'
server_port = 4551
channel = '134.ogg' # Lo indica el source
source_IP = '150.214.150.68'
source_port = 4552
header_size = 1024*20*10
block_size = 1024 # <- Ojo, valor recibido desde la fuente

# {{{ Args handing

parser = argparse.ArgumentParser(description='This is the drain node of a P2PSP cluster.')
parser.add_argument('--buffer_size', help='size of the video buffer in blocks'.format(buffer_size))
#parser.add_argument('--cluster_port', help='port used to communicate with the cluster. (Default = {})'.format(cluster_port))
parser.add_argument('--clients_port', help='Port used to communicate with the player. (Default = {})'.format(clients_port))
parser.add_argument('--channel', help='Name of the channel served by the streaming server. (Default = {})'.format(channel))
parser.add_argument('--server_IP', help='IP address of the streaming server. (Default = {})'.format(server_IP))
parser.add_argument('--server_port', help='Listening port of the streaming server. (Default = {})'.format(server_port))
parser.add_argument('--source_IP', help='IP address of the source. (Default = {})'.format(source_IP))
parser.add_argument('--source_port', help='Listening port of the source. (Default = {})'.format(source_port))

args = parser.parse_known_args()[0]
if args.buffer_size:
    buffer_size = int(args.buffer_size)
if args.clients_port:
    clients_port = int(args.player_port)
#if args.cluster_port:
#    cluster_port = int(args.cluster_port)
if args.channel:
    channel = args.channel
if args.server_IP:
    server_IP = args.server_IP
if args.server_port:
    server_port = int(args.server_port)
if args.source_IP:
    source_IP = args.source_IP
if args.source_port:
    source_port = args.source_port

# }}}
server = (server_IP, server_port)
source = (source_IP, source_port)
# {{{ Connect with the source

source_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
source_sock.connect(source)
if __debug__:
    print strftime("[%Y-%m-%d %H:%M:%S]", gmtime()), \
        source_sock.getsockname(), "connected to the source"

# }}}
# {{{ Transform the peer-source TCP socket into a UDP socket

cluster_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
cluster_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
print source_sock.getsockname()[PORT]
cluster_sock.bind(('',source_sock.getsockname()[PORT]))
print cluster_sock.getsockname()
source_sock.close()

# }}}
# {{{ Receive blocks from the source/peers

block_buffer = [None]*buffer_size
block_numbers = [0]*buffer_size
block_number = 0 # Last received block

#cluster_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#cluster_sock.bind(('', cluster_port))

lock = Lock()

class Receive_blocks(threading.Thread):

    def __init__(self, cluster_sock):
        threading.Thread.__init__(self)
        self.sock = cluster_sock
    
    def run(self):
        while True:
            lock.acquire()
            message, sender = self.sock.recvfrom(struct.calcsize("H1024s"))
            number, block = struct.unpack("H1024s", message)
            block_number = socket.ntohs(number)
            if __debug__:
                print strftime("[%Y-%m-%d %H:%M:%S]", gmtime()), \
                    sender, block_number, Color.green + "->" + Color.none, \
                    self.sock.getsockname()
            block_buffer[block_number % buffer_size] = block
            block_numbers[block_number % buffer_size] = block_number
            lock.release()

Receive_blocks(cluster_sock).start()

# }}}
# {{{ Serve the clients

class Client_handler(threading.Thread):

    def __init__(self, client):
        threading.Thread.__init__(self)
        self.client_sock, self.client_addr = client

    def run(self):
        global block_buffer
        global block_number
        # {{{ Create a TCP socket to Icecast

        #server_sock = blocking_socket(socket.AF_INET, socket.SOCK_STREAM)
        server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        print server
        server_sock.connect(server)
        server_sock.sendall("GET /" + channel + " HTTP/1.1\r\n\r\n")
        print "Client_hander:", "!"

        # }}}
        # {{{ Receive the video header from Icecast and send it to the client

        data = server_sock.recv(header_size)
        total_received = len(data)
        self.client_sock.sendall(data)
        while total_received < header_size:
            data = server_sock.recv(header_size - len(data))
            self.client_sock.sendall(data)
            total_received += len(data)

        print "Client_hander:", "header"

        # }}}
        # {{{ Close the TCP socket with the streaming server

        server_sock.close()

        # }}}
        # {{{ Now, send buffer's blocks to the client, forever
        block_to_play = block_number - buffer_size/2
        while True:
            lock.acquire()
            print len(block_buffer[block_to_play % buffer_size])
            #print block_buffer[block_to_play % buffer_size]
            self.client_sock.sendall(block_buffer[block_to_play % buffer_size])
            block_to_play = (block_to_play + 1) % 65536
            lock.release()

        # }}}

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(('', clients_port))
sock.listen(0)
if __debug__:
    print "Waiting for clients "

while True: # Serve forever.
    client = sock.accept()
    if __debug__:
        print "\bc",
    Client_handler(client).start()

# }}}
