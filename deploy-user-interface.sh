# Script for building the client-side user interface files,
# and deploying them to a local instance of LaBB-CAT,
# which is install in the location specified by the first parameter, or:
LOCAL_LABBCAT=${1:-/var/lib/tomcat9/webapps/labbcat}

if (mvn package -pl :nzilbb.labbcat.user-interface)
then
    if [ ! -d $LOCAL_LABBCAT/user-interface ]; then
        mkdir $LOCAL_LABBCAT/user-interface;
    fi
    rm -rf $LOCAL_LABBCAT/user-interface/*
    cp -r user-interface/target/labbcat-view/* $LOCAL_LABBCAT/user-interface/
    
    if [ ! -d $LOCAL_LABBCAT/edit/user-interface ]; then
        mkdir -p $LOCAL_LABBCAT/edit/user-interface;
    fi
    rm -rf $LOCAL_LABBCAT/edit/user-interface/*
    cp -r user-interface/target/labbcat-edit/* $LOCAL_LABBCAT/edit/user-interface/
    
    if [ ! -d $LOCAL_LABBCAT/admin/user-interface ]; then
        mkdir -p $LOCAL_LABBCAT/admin/user-interface;
    fi
    rm -rf $LOCAL_LABBCAT/admin/user-interface/*
    cp -r user-interface/target/labbcat-admin/* $LOCAL_LABBCAT/admin/user-interface/
fi
