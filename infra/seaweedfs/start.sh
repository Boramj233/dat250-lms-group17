#!/bin/sh
set -eu
# Only URL-safe characters are accepted so interpolation cannot break the JSON.
: "${LMS_STORAGE_ACCESS_KEY:?Set LMS_STORAGE_ACCESS_KEY}"
: "${LMS_STORAGE_SECRET_KEY:?Set LMS_STORAGE_SECRET_KEY}"
case "$LMS_STORAGE_ACCESS_KEY$LMS_STORAGE_SECRET_KEY" in
  *[!a-zA-Z0-9_-]*) echo 'Storage keys must use letters, digits, underscore or hyphen' >&2; exit 1 ;;
esac
umask 077
cat > /tmp/lms-s3.json <<JSON
{"identities":[{"name":"lms-submissions","credentials":[{"accessKey":"$LMS_STORAGE_ACCESS_KEY","secretKey":"$LMS_STORAGE_SECRET_KEY"}],"actions":["Read:lms-submissions","Write:lms-submissions","List:lms-submissions"]}]}
JSON
exec weed mini -dir=/data -bucket=lms-submissions -s3.config=/tmp/lms-s3.json -s3.autoCreateBucket=false -webdav=false -admin.ui=false -s3.port.iceberg=0 -s3.port.lance=0
