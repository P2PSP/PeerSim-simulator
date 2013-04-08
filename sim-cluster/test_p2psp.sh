xterm -e "./splitter.py" &
xterm -e './gatherer.py --splitter_name="localhost"' &
vlc http://localhost:9999 &
