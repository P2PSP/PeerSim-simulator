#!/usr/bin/python
# -*- coding: iso-8859-15 -*-

# Note: if you run the python interpreter in the optimzed mode (-O),
# debug messages will be disabled.

# {{{ GNU GENERAL PUBLIC LICENSE

# This is the gatherer node of the P2PSP (Peer-to-Peer Simple Protocol)
# <https://launchpad.net/p2psp>.
#
# Copyright (C) 2013 Vicente González Ruiz.
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

# Try running me as:
#
# ./splitter.py --source_hostname="localhost"
# ./gatherer.py --splitter_hostname="localhost" --source_hostname="localhost"
# vlc http://localhost:9999 &

# {{{ Imports

import logging
import os
from colors import Color
import sys
import socket
import struct
import argparse

# }}}

IP_ADDR = 0
PORT = 1

# Number of blocks of the buffer
buffer_size = 32

#cluster_port = 0 # OS default behavior will be used for port binding

# Port to communicate with the player
listening_port = 9999

# Number of bytes of the stream's header
header_size = 1024*20*10
#header_size = 1024*20

# Splitter endpoint
#splitter_hostname = '150.214.150.68'
splitter_hostname = 'localhost'
splitter_port = 4552

# Estas cuatro variables las debería indicar el splitter
channel = '134.ogg'
block_size = 1024
# Source's end-point
#source_hostname = '150.214.150.68'
source_hostname = 'localhost'
source_port = 4551

logging_levelname = 'INFO' # 'DEBUG' (default), 'INFO' (cyan),
                           # 'WARNING' (blue), 'ERROR' (red),
                           # 'CRITICAL' (yellow)
logging_level = logging.INFO

# {{{ Args handing

print 'Argument List:', str(sys.argv)

parser = argparse.ArgumentParser(
    description='This is the gatherer node of a P2PSP network.')

parser.add_argument('--buffer_size',
                    help='size of the video buffer in blocks'.format(buffer_size))

parser.add_argument('--channel',
                    help='Name of the channel served by the streaming source. (Default = {})'.format(channel))

parser.add_argument('--listening_port',
                    help='Port used to communicate with the player. (Default = {})'.format(listening_port))

parser.add_argument('--logging_levelname',
                    help='Name of the channel served by the streaming source. (Default = "{}")'.format(logging_levelname))

parser.add_argument('--source_hostname',
                    help='Hostname of the streaming source. (Default = {})'.format(source_hostname))

parser.add_argument('--source_port',
                    help='Listening port of the streaming source. (Default = {})'.format(source_port))

parser.add_argument('--splitter_hostname',
                    help='Hostname of the splitter. (Default = {})'.format(splitter_hostname))

parser.add_argument('--splitter_port',
                    help='Listening port of the splitter. (Default = {})'.format(splitter_port))

args = parser.parse_known_args()[0]
if args.buffer_size:
    buffer_size = int(args.buffer_size)
if args.channel:
    channel = args.channel
if args.listening_port:
    listening_port = int(args.listening_port)
if args.logging_levelname == 'DEBUG':
    logging_level = logging.DEBUG
if args.logging_levelname == 'INFO':
    logging_level = logging.INFO
if args.logging_levelname == 'WARNING':
    logging_level = logging.WARNING
if args.logging_levelname == 'ERROR':
    logging_level = logging.ERROR
if args.logging_levelname == 'CRITICAL':
    logging_level = logging.CRITICAL
if args.source_hostname:
    source_hostname = args.source_hostname
if args.source_port:
    source_port = int(args.source_port)
if args.splitter_hostname:
    splitter_hostname = args.splitter_hostname
if args.splitter_port:
    splitter_port = args.splitter_port

# }}}

print 'This is a P2PSP gatherer node ...',
if __debug__:
    print 'running in debug mode'
else:
    print 'running in release mode'

# {{{ Debugging initialization

# create logger
logger = logging.getLogger('gatherer (' + str(os.getpid()) + ')')
logger.setLevel(logging_level)

# create console handler and set the level
ch = logging.StreamHandler()
ch.setLevel(logging_level)

# create formatter
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# add formatter to ch
ch.setFormatter(formatter)

# add ch to logger
logger.addHandler(ch)

# }}}

source = (source_hostname, source_port)
splitter = (splitter_hostname, splitter_port)

def get_player_socket():
    # {{{

    #sock = blocking_TCP_socket.blocking_TCP_socket(socket.AF_INET, socket.SOCK_STREAM)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        # In Windows systems this call doesn't work!
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except:
        pass
    sock.bind(('', listening_port))
    sock.listen(0)
    
    logger.info(Color.cyan + '{}'.format(sock.getsockname()) +
                ' waiting for the player on port ' +
                str(listening_port) + Color.none)
    # }}}
        
    #sock, player = sock.baccept()
    sock, player = sock.accept()
    sock.setblocking(0)
    return sock

    # }}}
player_sock = get_player_socket() # The gatherer is blocked until the
                                  # player establish a connection.
# {{{ debug

if __debug__:
    logger.debug(Color.cyan + '{}'.format(player_sock.getsockname()) +
                 ' The player ' +
                 '{}'.format(player_sock.getpeername()) +
                 ' has establised a connection' + Color.none)

def communicate_the_header():
    # {{{ 
    source_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    source_sock.connect(source)
    source_sock.sendall("GET /" + channel + " HTTP/1.1\r\n\r\n")
    
    # {{{ Receive the video header from the source and send it to the player

    # Nota: este proceso puede fallar si durante la recepción de los
    # bloques el stream se acaba. Habría que realizar de nuevo la
    # petición HTTP (como hace el servidor).

    logger.info(Color.cyan +
                str(source_sock.getsockname()) + 
                ' retrieving the header ...' +
                Color.none)

    data = source_sock.recv(header_size)
    total_received = len(data)
    player_sock.sendall(data)
    while total_received < header_size:
        if __debug__:
            logger.debug(str(total_received))
        data = source_sock.recv(header_size - len(data))
        player_sock.sendall(data)
        total_received += len(data)

    # }}}

    logger.info(Color.cyan  +
                str(source_sock.getsockname()) +
                ' done' + Color.none)

    source_sock.close()
    # }}}


communicate_the_header() # Retrieve the header of the stream from the
                         # source and send it to the player.

# {{{ debug
if __debug__:
    logger.debug(" Trying to connect to the splitter at" + str(splitter))
# }}}

def connect_to_the_splitter():
    # {{{
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(splitter)
    return sock

    # }}}
splitter_sock = connect_to_the_splitter() # Connect to the splitter in
                                          # order to tell it who the
                                          # gatherer is.
splitter = splitter_sock.getpeername() # "localhost" -> "127.0.0.1"

logger.info(Color.cyan +
            '{}'.format(splitter_sock.getsockname()) +
            ' connected to the splitter' +
            Color.none)

# {{{ The gatherer is always the first node to connect to the splitter
# and therefore, in this momment the cluster is formed only by the
# splitter and the gatherer. So, it is time to create a new socket to
# receive blocks (now from the splitter and after, when at least one
# peer be in the cluster, from the peer(s) of the cluster), but that
# uses the UDP. This is called "cluster_sock". We also close the TCP
# socket that the gatherer has used to connect to the splitter.  }}}

def create_cluster_sock():
    # {{{

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # In Windows systems this call doesn't work!
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except:
        pass
    sock.bind(('',splitter_sock.getsockname()[PORT]))
    return sock

   # }}}
cluster_sock = create_cluster_sock()
cluster_sock.settimeout(1)
splitter_sock.close()

# {{{ We define the buffer structure. Three components are needed: (1)
# the <blocks> buffer that stores the received blocks, (2) the
# <numbers> buffer that stores the number of the blocks and (3) the
# <received> buffer that stores if a block has been received or not.
# }}}
blocks = [None]*buffer_size
received = [False]*buffer_size
#numbers = [0]*buffer_size

def receive():
    # {{{

    try:
        message, sender = cluster_sock.recvfrom(struct.calcsize("H1024s"))
        number, block = struct.unpack("H1024s", message)
        block_number = socket.ntohs(number)
        blocks[block_number % buffer_size] = block
        received[block_number % buffer_size] = True

        # {{{ debug
        if __debug__:
            logger.debug('{}'.format(cluster_sock.getsockname()) +
                          " <- " +
                          '{}'.format(block_number) +
                          ' ' +
                          '{}'.format(sender))
        # }}}

        return block_number
    except socket.timeout:
        logger.warning(Color.blue + "cluster timeout!" + Color.none) 
        return -1

    # }}}

# {{{ Now, here the gatherer's life begins (receive incomming blocks and
# send them to the player). But in order to handle the jitter, we must
# to prefetch some blocks before to start to send them. (Week 4/5)
# }}}

# Lets buffer data in order to handle the jitter. By default, we
# prefetch up to the half of the buffer. This should handle a jitter
# smaller or equal than the half of the buffer (measured in time).

# {{{ debug
if __debug__:
    logger.debug(str(cluster_sock.getsockname()) + ' buffering ...')
# }}}

logger.info(Color.cyan +
            str(cluster_sock.getsockname()) +
            ' receiving data ...' +
            Color.none)

'''
x = block_to_play = receive_a_block()
while not received[(x+buffer_size/2) % buffer_size]:
    x = receive_a_block()
'''
'''
block_to_play = receive_a_block() % buffer_size
while not received[(receive_a_block() + buffer_size/2) % buffer_size]:
    pass
'''

block_number = receive()
while block_number<=0:
    block_number = receive()
block_to_play = block_number % buffer_size
for x in xrange(buffer_size/2):
    while receive()<=0:
        pass

# {{{ debug
if __debug__:
    logger.debug(str(cluster_sock.getsockname()) + ' buffering done')
# }}}

def send_a_block_to_the_player():
    # {{{

    global block_to_play
    '''
    while not received[(block_to_play % buffer_size)]:
        message = struct.pack("!H", block_to_play)
        cluster_sock.sendto(message, splitter)
        '''
    if not received[block_to_play]:
        message = struct.pack("!H", block_to_play)
        cluster_sock.sendto(message, splitter)

        logger.info(Color.cyan +
                    str(cluster_sock.getsockname()) +
                    ' complaining about lost block ' +
                    str(block_to_play) +
                    Color.none)

        # La mayoría de los players se sincronizan antes si en lugar
        # de no enviar nada se envía un bloque vacío. Esto habría que
        # probarlo.

    try:
        player_sock.sendall(blocks[block_to_play])

        # {{{ debug
        if __debug__:
            logger.debug('{}'.format(player_sock.getsockname()) +
                          ' ' +
                          str(block_to_play) +
                          ' -> (player) ' +
                          '{}'.format(player_sock.getpeername()))

        # }}}

    except socket.error:
        logger.error(Color.red + 'player disconnected!' + Color.none)
        quit()
    except Exception as detail:
        logger.error(Color.red + 'unhandled exception ' + str(detail) + Color.none)

    received[block_to_play] = False

    # }}}

while True:
    block_number = receive()
    send_a_block_to_the_player()
    block_to_play = (block_to_play + 1) % buffer_size

'''
while True:
    send_a_block_to_the_player()
    block_to_play = (block_to_play + 1) % buffer_size
    receive()
'''
