#!/usr/bin/python
# -*- coding: iso-8859-15 -*-

# Note: if you run the python interpreter in the optimzed mode (-O),
# debug messages will be disabled.

# {{{ GNU GENERAL PUBLIC LICENSE

# This is the splitter node of the P2PSP (Peer-to-Peer Simple Protocol)
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
# xterm -e "./splitter.py" &
# xterm -e './gatherer.py --splitter_hostname="localhost"' &
# vlc http://localhost:9999 &

'''
# VERSIÓN bloque-exclusivo DEL SPLITTER. 
# El splitter envía un bloque de stream exclusivo a cada peer entrante. El peer reenvía dicho bloque a todos a modo de "hola". 
# Con esto prentendemos acelerar el proceso de buffering y saturar menos la red.
# Usar con peer-x.py y gatherer.py
'''

# {{{ imports

import logging
from colors import Color
import socket
from blocking_TCP_socket import blocking_TCP_socket
import sys
import struct
import time
#import thread
from threading import Thread
from threading import Lock
from threading import RLock
from time import gmtime, strftime
import os
import argparse

# }}}

total_blocks = 1    #starts in 1 to avoid div-by-zero issues when calculating the percentage
total_blocks = long(total_blocks)   #to declare it long. Alternatively: total_blocks = 0L
loss_percentage = 0
loss_percentage = float(loss_percentage)  #the same with the percentage of loss 

IP_ADDR = 0
PORT = 1


buffer_size = 32    # Buffer size in the peers and the gatherer
block_size = 1024
channel = '134.ogg'
#source_hostname = '150.214.150.68'
source_hostname = 'localhost'
source_port = 4551
listening_port = 4552

logging_levelname = 'INFO' # 'DEBUG' (default), 'INFO' (cyan),
                           # 'WARNING' (blue), 'ERROR' (red),
                           # 'CRITICAL' (yellow)
logging_level = logging.INFO

# {{{ Args handing

print 'Argument List:', str(sys.argv)

parser = argparse.ArgumentParser(
    description='This is the splitter node of a P2PSP network.')

parser.add_argument('--buffer_size',
                    help='size of the video buffer in blocks'.format(buffer_size))

parser.add_argument('--block_size',
                    help='Block size in bytes. (Default = {})'.format(block_size))

parser.add_argument('--channel',
                    help='Name of the channel served by the streaming source. (Default = "{}")'.format(channel))

parser.add_argument('--logging_levelname',
                    help='Name of the channel served by the streaming source. (Default = "{}")'.format(logging_levelname))

parser.add_argument('--source_hostname',
                    help='Hostname of the streaming server. (Default = "{}")'.format(source_hostname))

parser.add_argument('--source_port',
                    help='Listening port of the streaming server. (Default = {})'.format(source_port))

parser.add_argument('--listening_port',
                    help='Port to talk with the gatherer and peers. (Default = {})'.format(listening_port))

args = parser.parse_known_args()[0]
if args.buffer_size:
    buffer_size = int(args.buffer_size)
    print("Buffer size "+str(buffer_size))
if args.block_size:
    block_size = int(args.block_size)
if args.channel:
    channel = args.channel
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
    source_hostname = str(args.source_hostname)
if args.source_port:
    source_port = int(args.source_port)
if args.listening_port:
    listening_port = int(args.listening_port)

# }}}

print 'This is a P2PSP splitter node ...',
if __debug__:
    print 'running in debug mode'
else:
    print 'running in release mode'

# {{{ Logging initialization

# Echar un vistazo a logging.config.

# create logger
logger = logging.getLogger('splitter (' + str(os.getpid()) + ')')
logger.setLevel(logging_level)

# create console handler and set the level
ch = logging.StreamHandler()
ch.setLevel(logging_level)

# create formatter
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
#formatter = logging.Formatter("%(asctime)s [%(funcName)s: %(filename)s,%(lineno)d] %(message)s")

# add formatter to ch
ch.setFormatter(formatter)

# add ch to logger
logger.addHandler(ch)

#logging.basicConfig(format='%(asctime)s.%(msecs)d %(message)s',
#                    datefmt='%H:%M:%S',
#                    level=logging.DEBUG)
#    logging.basicConfig(format='%(asctime)s.%(msecs)d %(message)s',
#                        datefmt='%H:%M:%S')
#  else:
#    print 'Running in release mode'
#    logging.basicConfig(format='%(asctime)s.%(msecs)d %(message)s',
#                        datefmt='%H:%M:%S',
#                        level=logging.CRITICAL)

# }}}

source = (source_hostname, source_port)

# }}} 
               
# The list of peers (included the gatherer)
peer_list = [] 

# The number of the last received block from the streaming server
block_number = 0 

# Used to find the peer to which a block has been sent
destination_of_block = [('0.0.0.0',0) for i in xrange(buffer_size)]

# Unreliability rate of a peer
unreliability = {}

# Complaining rate of a peer
complains = {}

# The peer_list iterator
peer_index = 0

# A lock to perform mutual exclusion for accesing to the list of peers
peer_list_lock = Lock()
# A lock for source_sock
source_sock_lock = Lock()

gatherer = None

logger.info("Buffer size: "+str(buffer_size)+" blocks")

# {{{ Handle one telnet client

class get_the_state(Thread):
    # {{{

    global peer_list

    def __init__(self):
        Thread.__init__(self)

    def run(self): 
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            # This does not work in Windows systems.
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        except:
            pass
        sock.bind(('', listening_port+1))

        logger.info(Color.cyan +
                    '{}'.format(sock.getsockname()) +
                    ' listening (telnet) on port ' +
                    str(listening_port+1) +
                    Color.none)

        sock.listen(0)
        try:
            while True:
                connection = sock.accept()[0]
                message = 'a'
                while message[0] != 'q':
                    connection.sendall('Gatherer = ' + str(gatherer) + '\n')
                    connection.sendall('Number of peers = ' + str(len(peer_list)) + '\n')
                    counter = 0
                    for p in peer_list:
                        loss_percentage = float(unreliability[p]*100)/float(total_blocks)
                        connection.sendall(str(counter) +
                                           '\t' + str(p) +
                                           '\t' + 'unreliability=' + str(unreliability[p]) +' ({:.2}%)'.format(loss_percentage)+
                                           '\t' + 'complains=' + str(complains[p]) +
                                           '\n')
                        counter += 1
                    connection.sendall('\n Total blocks sent = '+str(total_blocks))
                    connection.sendall(Color.cyan + '\nEnter a line that beggings with "q" to exit or any other key to continue\n' + Color.none)
                    message = connection.recv(2)

                connection.close()

        except:
            pass

get_the_state().setDaemon(True)
get_the_state().daemon=True
get_the_state().start()

# }}}

# Return the connection socket used to establish the connections of the
# peers (and the gatherer) (Week 3)

def get_peer_connection_socket():
    #sock = blocking_TCP_socket(socket.AF_INET, socket.SOCK_STREAM)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    try:
        # This does not work in Windows systems.
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except:
        pass

    sock.bind( ('', listening_port) )
    sock.listen(5)

    return sock

peer_connection_sock = get_peer_connection_socket()

logger.info(Color.cyan +
            '{}'.format(peer_connection_sock.getsockname()) +
            ' waiting for the gatherer on port ' +
            str(listening_port) +
            Color.none)

gatherer = peer_connection_sock.accept()[1]

logger.info(Color.cyan +
            '{}'.format(peer_connection_sock.getsockname()) +
            ' the gatherer is ' +
            str(gatherer) +
            Color.none)

# {{{ Handle peer arrivals.

class handle_arrivals(Thread):
    # {{{

    def __init__(self):
        Thread.__init__(self)

    def run(self):
        #peer_connection_sock = blocking_TCP_socket(socket.AF_INET, socket.SOCK_STREAM)
        #peer_connection_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        #peer_connection_sock.bind(("", listening_port)) # Listen to any interface
        #peer_connection_sock.listen(5)
        #global peer_connection_sock
        
        #temp_peer_list = []
        
        global block_number
        global total_blocks
        global destination_of_block
        global unreliability
        global complains
        
        while True:
            # {{{ Wait for the connection from the peer /PS0/

            peer_serve_socket, peer = peer_connection_sock.accept()

            # {{{ debug
            if __debug__:
                logger.debug('{}'.format(peer_serve_socket.getsockname()) +
                             ' Accepted connection from peer ' +
                             str(peer))
            # }}}

            # }}}

            # {{{ Send the last block to the peer /PS3/

            # Solicitar un nuevo bloque a Icecast y enviárselo al peer
            #block = block_buffer[last_block % buffer_size]
            #payload = struct.pack("H1024s", socket.htons(last_block), block)
            #peer_serve_socket.sendall(payload)

            # }}}

            # {{{ Send the list of peers to the peer /PS4/

            # {{{ debug

            if __debug__:
                logger.debug('{}'.format(peer_serve_socket.getsockname()) +
                             ' Sending the list of peers')
            # }}}

            try:
                block = receive_next_block()
                
                # {{{ debug
                if __debug__:
                    logger.debug('{}'.format(source_sock.getsockname()) +
                     Color.green + ' <- ' + Color.none +
                     '{}'.format(source_sock.getpeername()) + " (source)" +
                     ' ' +
                     '{}'.format(block_number))
                # }}}


                try:
                    peer_list_lock.acquire()    #get the lock
                    #temp_peer_list = copy.copy(peer_list)
                    temp_peer_list = list(peer_list) #http://henry.precheur.org/python/copy_list
                    
                    peer_list.append(peer)
                    temp_block_number = block_number    #for later use outside the critical secion
                    block_number = (block_number + 1) % 65536 #update block number prematurely. This is so because we don't want to acquire the lock twice.
                    total_blocks += 1
                    destination_of_block[block_number % buffer_size] = peer
                except Exception:
                    print("Exception adding the peer to the peer list in handle arrivals")
                finally:
                    peer_list_lock.release()    #release the lock
                    
                if __debug__:
                    #use block_number-1, since we updated block_number prematurely in the lock.
                    logger.debug("First block sent to peer "+str(peer)+" : "+str(block_number-1))
                    logger.debug("First block sent to peer "+str(peer)+" in buffer position : "+str((block_number-1)%buffer_size))
                
                unreliability[peer] = 0
                complains[peer] = 0
                         
                #send the block
                #message = struct.pack("H1024s", socket.htons(block_number-1), block) #use block_number-1, since we updated block_number prematurely in the lock.
                message = struct.pack("H1024s", socket.htons(temp_block_number), block) 
                peer_serve_socket.sendall(message)

                #send the list of peers
                message = struct.pack("H", socket.htons(len(temp_peer_list)))
                peer_serve_socket.sendall(message)
                message = struct.pack("4sH", socket.inet_aton(gatherer[IP_ADDR]),socket.htons(gatherer[PORT]))
                peer_serve_socket.sendall(message)
                for p in temp_peer_list:
                    message = struct.pack("4sH", socket.inet_aton(p[IP_ADDR]),socket.htons(p[PORT]))
                    peer_serve_socket.sendall(message)
            
                # {{{ debug
    
                if __debug__:
                    logger.debug(str(len(temp_peer_list)) + ' peers sent (plus gatherer)')
    
                # }}}
    
                # }}}
    
                # {{{ Close the TCP socket with the peer/gatherer
    
                peer_serve_socket.close()
    
                # }}}
    
                # Then the first peer arrival, the first entry of the list
                # of peers is replaced by the peer.
                #if peer_list[0] == gatherer:
                #    peer_list[0] = peer
                #else:
                #with peer_list_lock:
                #peer_list_lock.acquire()
                #peer_list.append(peer)
                #peer_list_lock.release()
                
            

                logger.info(Color.cyan +
                        str(peer) +
                        ' has joined the cluster' +
                        Color.none)
            except:
                print("Exception in handle_arrivals")
            

    # }}}

handle_arrivals().setDaemon(True) 
handle_arrivals().daemon=True
handle_arrivals().start()

# }}}

# {{{ Create the socket to send the blocks of stream to the peers/gatherer

def create_cluster_sock(listening_port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # This does not work in Windows systems.
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    except:
        pass
    sock.bind(('', listening_port))
    #peer_socket.bind(('',peer_connection_sock.getsockname()[PORT]))

    return sock

cluster_sock = create_cluster_sock(listening_port)

# }}}

# {{{ Handle peer/gatherer complains and goodbye messages (Week 10)

class listen_to_the_cluster(Thread):
    # {{{

    def __init__(self):
        Thread.__init__(self)

    def run(self):

        global peer_index

        while True:
            # {{{ debug
            if __debug__:
                logger.debug('waiting for messages from the cluster')
            # }}}
            message, sender = cluster_sock.recvfrom(struct.calcsize("H"))

            #if len(message) == 0:
            if message == 'bye':
                try:
                    peer_list_lock.acquire()    #get the lock
                    peer_list.remove(sender)
                    logger.info(Color.cyan + str(sender) + ' has left the cluster' + Color.none)
                except:
                    logger.warning(Color.blue + 'Received a googbye message from ' + str(sender) + ' which is not in the list of peers' + Color.none)
                    pass
                finally:
                    peer_list_lock.release()    #release the lock
            else:
                # The sender of the packet complains and the packet
                # comes with the index of a lost block
                try:
                    peer_list_lock.acquire()    #get the lock
                    lost_block = struct.unpack("!H",message)[0]
                    destination = destination_of_block[lost_block]

                    logger.debug(Color.cyan + str(sender) + ' complains about lost block ' + str(lost_block) + ' sent to ' + str(destination) + Color.none)
                    unreliability[destination] += 1
                finally:
                    peer_list_lock.release()    #release the lock
                    
'''jalvaro: i'm commenting this so peers are not expeled
#if the sender of the complaint is the gatherer then the splitter removes the infractor inmediately
                if sender == gatherer:
                    try:
                        peer_list.remove(destination)
                        del unreliability[destination]
                        del complains[destination]
                        
                        logger.info(Color.cyan +
                                    str(destination) +
                                    ' has been removed' +
                                    Color.none)
                    except:
                        pass

                else:
                    try:
                        unreliability[destination] += 1
                        if unreliability[destination] > len(peer_list):
                            # Too many complains about an unsuportive peer
                            peer_list.remove(destination)
                            del unreliability[destination]
                            del complains[destination]

                            logger.info(Color.cyan +
                                        str(destination) +
                                        ' has been removed' +
                                        Color.none)

                    except:
                        pass
'''
                
    # }}}

listen_to_the_cluster().setDaemon(True)
listen_to_the_cluster().daemon=True
listen_to_the_cluster().start()

# }}}

# {{{ Connect to the streaming server and request the channel (week 2)

source_sock = blocking_TCP_socket(socket.AF_INET, socket.SOCK_STREAM)
source_sock.connect(source)

# {{{ debug

if __debug__:
    logger.debug('{}'.format(source_sock.getsockname()) +
                 ' connected to the video source ' +
                 '{}'.format(source_sock.getpeername()))

# }}}

# {{{ Request the video to the source

GET_message = 'GET /' + channel + " HTTP/1.1\r\n\r\n"
source_sock.sendall(GET_message)

# }}}

# {{{ debug

if __debug__:
    logger.debug('{}'.format(cluster_sock.getsockname()) +
                 ' sending the rest of the stream ...')

# }}}

# {{{ Feed the peers

while True:

    # (Week 2)
    def receive_next_block():
        # {{{

        global source_sock

        source_sock_lock.acquire()  #get the lock
        try:
            block = source_sock.recv(block_size)
            tries = 0
            while len(block) < block_size:
                tries += 1
                if tries > 3:
    
                    # {{{ debug
                    if __debug__:
                        logger.debug('GET')
                    # }}}
    
                    time.sleep(1)
                    source_sock.close()
                    source_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    source_sock.connect(source)
                    source_sock.sendall(GET_message)
    
                block += source_sock.recv(1024-len(block))
        finally:
            source_sock_lock.release()  #release the lock
        return block

        # }}}

    block = receive_next_block()
    #block = source_sock.brecv(block_size)

    # {{{ debug
    if __debug__:

        logger.debug('{}'.format(source_sock.getsockname()) +
                     Color.green + ' <- ' + Color.none +
                     '{}'.format(source_sock.getpeername()) + " (source)" +
                     ' ' +
                     '{}'.format(block_number))
    # }}}

    '''
    Nuevo código
    '''
    peer_list_lock.acquire()    #get peer_list_lock
    try:
        len_peer_list = len(peer_list)
        try:
            peer = peer_list[peer_index]
            #logger.debug('{}'.format(cluster_sock.getsockname())+Color.green+' -> '+Color.none+ str(peer)+' (peer) '+str(block_number))
        except:
            try:
                peer = peer_list[0]
                #logger.debug('{}'.format(cluster_sock.getsockname())+Color.green+' -> '+Color.none+ str(peer)+' (peer) '+str(block_number))
            except:
                peer = gatherer
                len_peer_list = 1
                #logger.debug('{}'.format(cluster_sock.getsockname())+Color.green+' -> '+Color.none+ str(peer)+' (gatherer) '+str(block_number))
        destination_of_block[block_number % buffer_size] = peer
        peer_index = (peer_index + 1) % len_peer_list
        temp_block_number = block_number #for later use outside the critical section
        block_number = (block_number + 1) % 65536   #update block number prematurely. This is so because we don't want to acquire the lock twice.
        total_blocks += 1
    finally:
        peer_list_lock.release()    # release peer_list_lock

    #message = struct.pack("H1024s", socket.htons(block_number-1), block)    #use block_number-1, since we updated block_number prematurely in the lock.
    message = struct.pack("H1024s", socket.htons(temp_block_number), block)    
    cluster_sock.sendto(message, peer)
    
    #use block_number-1, since we updated block_number prematurely in the lock.
    if peer == gatherer:
        logger.debug('{}'.format(cluster_sock.getsockname())+Color.green+' -> '+Color.none+ str(peer)+' (gatherer) '+str(block_number-1))
    else:
        logger.debug('{}'.format(cluster_sock.getsockname())+Color.green+' -> '+Color.none+ str(peer)+' (peer) '+str(block_number))
        
    '''
    Fin del nuevo código
    '''

    '''
    #Código antiguo
    #with peer_list_lock:
    #peer_list_lock.acquire()
    len_peer_list = len(peer_list)
    #if peer_index < len_peer_list:
    try:
        peer = peer_list[peer_index]
    except:
        try:
            peer = peer_list[0]
        except:
            peer = gatherer
            len_peer_list = 1
    #peer_list_lock.release()

    # {{{ debug
    if __debug__:
        logger.debug('{}'.format(cluster_sock.getsockname()) +
                     Color.green + ' -> ' + Color.none +
                     '{}'.format(peer) +
                     ' ' +
                     '{}'.format(block_number))
    # }}}

    message = struct.pack("H1024s", socket.htons(block_number), block)
    #if not (block_number%2)==0:
    cluster_sock.sendto(message, peer)
    # Ojo, a veces peta diciendo: "IndexError: list index out of range"
    destination_of_block[block_number % buffer_size] = peer

    peer_index = (peer_index + 1) % len_peer_list

    block_number = (block_number + 1) % 65536
    
    total_blocks += 1
    #Fin del código antiguo
    '''
    
    '''
    #decrement unreliability and complaints after every 256 packets
    if (block_number % 256) == 0:
        for i in unreliability:
            unreliability[i] /= 2
        for i in complains:
            complains[i] /= 2
    '''

# }}}
