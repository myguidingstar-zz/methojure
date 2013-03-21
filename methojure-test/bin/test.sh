#!/bin/bash

# move to the parent folder of this directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..

karma start --single-run $@
