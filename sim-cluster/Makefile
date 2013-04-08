BIN = ../bin

default:	all

M4 := $(shell which m4)

ifeq ($(M4),)
$(warning m4 not found!)
false
endif

$(BIN)/%.py:	%.py
		(echo "undefine(\`format')changequote({{,}})dnl"; cat $*.py) | sed 's/#ifdef/ifdef/' | sed 's/#)/)/' | sed 's/#,/,/' | m4 -D $(DEFS) > $@; chmod +x $@
#		cpp -D $(DEFS) < $*.py > $@; chmod +x $@

PYs := $(wildcard *.py)
EXEs += $(PYs:%.py=$(BIN)/%.py)

$(BIN)/%.sh:	%.sh
		cp $*.sh $@; chmod +x $@

SHs := $(wildcard *.sh)
EXEs += $(SHs:%.sh=$(BIN)/%.sh)

DEFS = _DEBUG_
#DEFS = _RELEASE_
#DEFS = _SIMULATION_

all:	$(EXEs)

info:
	@echo $(EXEs)

clean:
	rm -f $(EXEs)
