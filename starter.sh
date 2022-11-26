#!/bin/bash
file=$1
file_contents=$(cat $file)
javac ./kooToueg.java
n=$(echo $file_contents | cut -d ' ' -f 1)
minDelay=$(echo $file_contents | cut -d ' ' -f 2)
# echo $test
echo $n
echo $minDelay


for ((i=1; i<=$n; i++))
do
    node_id=$(echo $file_contents | cut -d ' ' -f $((3*i-1)))
    hostname=$(echo $file_contents | cut -d ' ' -f $((3*i)))
    port=$(echo $file_contents | cut -d ' ' -f $((3*i+1)))
    echo $node_id $hostname $port
    # code --new-window --reuse-window --terminal.integrated.shell.linux=/bin/bash --terminal.integrated.shellArgs.linux=-c --terminal.integrated.shellArgs.linux="java ./kooToueg $node_id"
done