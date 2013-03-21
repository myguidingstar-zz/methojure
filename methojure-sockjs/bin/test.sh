#!/bin/bash

MAIN=methojure.sockjs.test.protocol-test-server
LOG_FILE=test-server.log
LOG_STR='Protocol test server started.'

# move to the parent folder of this directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..

# start the server
lein run -m $MAIN > $LOG_FILE &

# wait for the server to start
retry=30
while [[ $retry -gt 0 ]] && ! grep -q "$LOG_STR" $LOG_FILE
do
    sleep 1
    retry=$(($retry - 1))
done

# exit we retry is small or equal zero
if [ $retry -le 0 ]
then
    exit 1
else
    # run tests
    karma start --single-run $@
    result=$?  
fi

# remove log file
rm -f $LOG_FILE

# stop the server
for i in `ps -ef | grep $MAIN | awk '{ print $2 }' | head -n -1`
do
    kill -9 $i
done

# return with error code
exit $result
