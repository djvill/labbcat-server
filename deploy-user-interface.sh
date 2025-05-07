# Script for building the client-side user interface files,
# and deploying them to a local instance of LaBB-CAT,
# which is install in the location specified by the first parameter, or:
LOCAL_LABBCAT=${1:-"/c/Program Files/Apache Software Foundation/Tomcat 9.0/webapps/apls-dev"}

if (mvn clean package -pl :nzilbb.labbcat.user-interface)
then
    if [ ! -d "$LOCAL_LABBCAT/user-interface" ]; then
        mkdir "$LOCAL_LABBCAT/user-interface";
    fi
    rm -rf "$LOCAL_LABBCAT/user-interface/*"
    cp -r user-interface/target/labbcat-view/browser/* "$LOCAL_LABBCAT/user-interface/"
    for svg in "$LOCAL_LABBCAT"/user-interface/*/assets/*svg ; do 
        sed -i 's/#859044/#FFB81C/g' "$svg"
    done
    
    if [ ! -d "$LOCAL_LABBCAT/edit/user-interface" ]; then
        mkdir -p "$LOCAL_LABBCAT/edit/user-interface";
    fi
    rm -rf "$LOCAL_LABBCAT/edit/user-interface/*"
    cp -r user-interface/target/labbcat-edit/browser/* "$LOCAL_LABBCAT/edit/user-interface/"
    for svg in "$LOCAL_LABBCAT"/edit/user-interface/*/assets/*svg ; do 
        sed -i 's/#859044/#FFB81C/g' "$svg"
    done
    
    if [ ! -d "$LOCAL_LABBCAT/admin/user-interface" ]; then
        mkdir -p "$LOCAL_LABBCAT/admin/user-interface";
    fi
    rm -rf "$LOCAL_LABBCAT/admin/user-interface/*"
    cp -r user-interface/target/labbcat-admin/browser/* "$LOCAL_LABBCAT/admin/user-interface/"
    for svg in "$LOCAL_LABBCAT"/admin/user-interface/*/assets/*svg ; do 
        sed -i 's/#859044/#FFB81C/g' "$svg"
    done
fi

