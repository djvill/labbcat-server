#!/usr/bin/env bash

# Script for building the client-side **view** files,
# and deploying them to a local instance of LaBB-CAT,
# adding "prev-" prefix to existing index.html

LOCAL_LABBCAT="/c/Program Files/Apache Software Foundation/Tomcat 9.0/webapps/apls-dev"
LL_UI="$LOCAL_LABBCAT/user-interface/en"

common=${1}
if [ -z $common ] ; then
  build_common=false
elif [ $common = "-c" ] || [ $common = "--common" ] ; then
  build_common=true
else
  echo Option can only be -c or --common
  echo "Syntax: bash deploy-view.sh [-c|--common]"
fi

if (
  if $build_common ; then
    ng build labbcat-common --configuration production &&
    ng build labbcat-view --configuration production
  else
    ng build labbcat-view --configuration production
  fi
  ) ; then
  mv "$LL_UI/index.html" "$LL_UI/prev-index.html"
  cd dist/labbcat-view/browser
  cp -t "$LL_UI" index.html *js *css
  cp media/* "$LL_UI/media"
  for svg in "$LL_UI"/media/*svg ; do
    if [[ "$svg" == "$LL_UI"/media/sine*.svg ]] ; then
      sed -i 's/#96a339/#003594/g' "$svg";
    else
      sed -i 's/#859044/#FFB81C/g' "$svg";
    fi
  done
  echo Built at $(date)
fi

