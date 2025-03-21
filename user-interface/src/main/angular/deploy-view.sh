#!/usr/bin/env bash

# Script for building the client-side **view** files,
# and deploying them to a local instance of LaBB-CAT,
# adding "prev-" prefix to existing index.html

LOCAL_LABBCAT=${1:-"/c/Program Files/Apache Software Foundation/Tomcat 9.0/webapps/apls-dev"}
LL_UI="$LOCAL_LABBCAT/user-interface/en"

if (
  ng build labbcat-common --configuration production && #comment-out if not rebuilding
  ng build labbcat-view --configuration production
  ) ; then
  mv "$LL_UI/index.html" "$LL_UI/prev-index.html"
  cd dist/labbcat-view/browser
  cp -t "$LL_UI" index.html *js *css
  cp media/* "$LL_UI/media"
fi
