#!/bin/sh

PROJECT_NAME=movies
PROJECT_SECRET=dm5VNS9I98ke7SAdk01M9JU2Ya7e11Gd
PROXY_USER=m@m.com

COMMAND="curl $PROJECT_NAME:$PROJECT_SECRET@localhost:8080/oauth/token -d \"grant_type=client_credentials&proxy_user=$PROXY_USER\" | jq"

echo ""
eval $COMMAND
echo ""
