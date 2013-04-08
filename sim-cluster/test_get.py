#!/usr/bin/python
# -*- coding: iso-8859-15 -*-

# {{{ GNU GENERAL PUBLIC LICENSE

# This is the splitter node of the P2PSP (Peer-to-Peer Simple Protocol)
# <https://launchpad.net/p2psp>.
#
# Copyright (C) 2013 Cristobal Medina López, Juan Pablo García Ortiz,
# Juan Alvaro Muñoz Naranjo, Leocadio González Casado and Vicente
# González Ruiz.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# }}}

# Test the connection with the streaming server.

# {{{ imports

import logging
import socket
from blocking_TCP_socket import blocking_TCP_socket
import sys
import struct
import time
from threading import Thread
from threading import Lock
from colors import Color
import signal
from time import gmtime, strftime
import os
import argparse

# }}}

IP_ADDR = 0
PORT = 1

block_size = 1024
channel = '134.ogg'
source_name = '150.214.150.68'
source_port = 4551
listening_port = 4552

# {{{ Args handing

parser = argparse.ArgumentParser(description='This a test_get of a P2PSP cluster.')
parser.add_argument('--block_size', help='Block size in bytes. (Default = {})'.format(block_size))
parser.add_argument('--channel', help='Name of the channel served by the streaming source. (Default = "{}")'.format(channel))
parser.add_argument('--source_name', help='Name of the streaming server. (Default = "{}")'.format(source_name))
parser.add_argument('--source_port', help='Listening port of the streaming server. (Default = {})'.format(source_port))
parser.add_argument('--listening_port', help='Port to talk with the drain and peers. (Default = {})'.format(listening_port))

args = parser.parse_known_args()[0]
if args.block_size:
    block_size = int(args.block_size)
if args.channel:
    channel = args.channel
if args.source_name:
    source_name = args.source_name
if args.source_port:
    source_port = int(args.source_port)
if args.listening_port:
    listening_port = int(args.listening_port)

source = (source_name, source_port)

# {{{ debug
if __debug__:
    print 'Running in debug mode'
    logging.basicConfig(format='%(asctime)s.%(msecs)d %(message)s',
                        datefmt='%H:%M:%S',
                        level=logging.WARNING)
else:
    print 'Running in release mode'
    logging.basicConfig(format='%(asctime)s.%(msecs)d %(message)s',
                        datefmt='%H:%M:%S',
                        level=logging.CRITICAL)
# }}}

# {{{ The drain is blocked until a player establish a connection. (Week 4/6)

def get_player_socket():

    # {{{

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        # In Windows systems this call doesn't work!
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except:
        pass
    sock.bind(('', listening_port))
    sock.listen(0)

    # {{{ debug
    if __debug__:
        logging.warning('{}'.format(sock.getsockname())
                        + ' Waiting for the player connection ...')
    # }}}

    sock, player = sock.accept()
    #sock.setblocking(0)
    return sock

    # }}}

player_sock = get_player_socket()

# }}}

source_sock = blocking_TCP_socket(socket.AF_INET, socket.SOCK_STREAM)
source_sock.connect(source)
source_sock.sendall("GET /" + channel + " HTTP/1.1\r\n\r\n")

header_size = 1000000

'''
data = source_sock.recv(header_size)
total_received = len(data)
player_sock.sendall(data)
while total_received < header_size:
    if __debug__:
        logging.warning('h')
    data = source_sock.recv(header_size - len(data))
    player_sock.sendall(data)
    total_received += len(data)
'''


block = source_sock.brecv(block_size)
total_received = len(block)
player_sock.sendall(block)
print total_received
while total_received < header_size:
    if __debug__:
        logging.warning(str(len(block)))
    block = source_sock.brecv(block_size)
    player_sock.sendall(block)
    total_received += block_size

