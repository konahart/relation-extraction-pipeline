#!/bin/bash
if [$# -lt 1]; then
    echo Usage: `basename $0` freebaseFile [outputDirectory] 1>&2
fi
if [$# -e 1]; then
    OUTPUTDIR=$2
else
    OUTPUTDIR="."
fi
FREEBASE=$1

#Getting both common.topic.alias and type.object.name returned too many results for
#MapBasedEntityLinker to handle, so just getting common.topic.alias for now
RELATIONS="(common\.topic\.alias)"

#Uncomment to use both common.topic.alias and type.object.name:
#RELATIONS="(common\.topic\.alias|type\.object\.name)"

FREEBASEFORMAT="<http:\/\/rdf\.freebase\.com\/ns\/([^>]+)>\s+<http:\/\/rdf\.freebase\.com\/ns\/${RELATIONS}>\s+\"([^\"]*)\".*"

#Use following form to limit to one language (e.g., en)
#FREEBASEFORMAT="<http:\/\/rdf\.freebase\.com\/ns\/([^>]+)>\s+<http:\/\/rdf\.freebase\.com\/ns\/${RELATIONS}>\s+\"([^\"]*)\"@en.*"

sed -rn "s/${FREEBASEFORMAT}/\1\t\3/p" ${FREEBASE} > "${OUTPUTDIR}/entityAliases"

exit 0 
