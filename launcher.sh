#!/bin/bash
pass=$1 
netid=mxk200008
PROJDIR=$HOME/CS6378/Project2/KooTouegProtocol
CONFIG=$PROJDIR/config.txt
PROG=$PROJDIR/kooToueg
n=0
cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read firstLine
    echo $firstLine

    numberOfServers=$( echo $firstLine | awk '{ print $1}' )

    while  [ $n -lt $numberOfServers ]
    do
        read line
        host=$( echo $line | awk '{ print $2 }' )
        sshpass -p $pass ssh -o StrictHostKeyChecking=no $netid@$host java $PROG $n $CONFIG > $PROJDIR/${n}.log &

        n=$(( n + 1 ))
    done
   
)

