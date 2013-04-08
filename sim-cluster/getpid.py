#!/usr/bin/python
# -*- coding: iso-8859-15 -*-

import os
import argparse
import daemon

parser = argparse.ArgumentParser(description='Testing getpid.')
parser.add_argument('--showpeers', help='Show the list of peers.')
args = parser.parse_known_args()[0]
if args.showpeers:
    print os.getpid()
