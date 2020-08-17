#!/bin/bash

if ! grep -q localhost nodes; then
    echo "localhost" >> nodes
    echo -en "Added localhost as first node"
fi

mpic++ source.cpp -o source &>/dev/null

mpirun -np 2 -hostfile nodes ./source