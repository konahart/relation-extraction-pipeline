#!/bin/bash
if [ $# -lt 2 ]; then
    echo Usage: `basename $0` freebaseFile relationListFile [outputDirectory] 1>&2
    exit 0
fi
if [ $# -ge 3 ]; then
    OUTPUTDIR=$3
else
    OUTPUTDIR=${PWD}
fi

FREEBASE=$1
RELATIONFILE=$2

readarray relations < $RELATIONFILE

for PATTERN in ${relations[@]}; do
    FREEBASEFORMAT="<http:\/\/rdf\.freebase\.com\/ns\/([^>]+)>\s+<http:\/\/rdf\.freebase\.com\/ns\/(${PATTERN})>\s+<http:\/\/rdf\.freebase\.com\/ns\/([^>]+)>\s+\."
    echo grepping $PATTERN
    echo outputting results to $OUTPUTDIR/freebase-$PATTERN
    sed -rn "s/${FREEBASEFORMAT}/\1\t\2\t\3/p" ${FREEBASE} > ${OUTPUTDIR}/freebase-$PATTERN
done
exit 0 
