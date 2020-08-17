#!/bin/bash
gcc -c -o aes.o aes.c

ar rcs aes.a aes.o

if ! grep -q localhost nodes; then
    echo "localhost" >> nodes
    echo -en "Added localhost as first node"
fi

mpic++ Source.cpp -o source -fopenmp aes.a &>/dev/null

echo -n "Enter the absolute path of the file you want to encrypt: "
read filename

echo -n "Enter the password - must have 16 characters: "
read password

echo -n "Insert number of processes: "
read np

mpirun -np $np -hostfile nodes ./source $filename $password 