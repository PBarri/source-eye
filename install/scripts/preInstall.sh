#!/bin/sh
echo "Creating group: sourceeye"
/usr/sbin/groupadd -f -r sourceeye 2> /dev/null || :

echo "Creating user: sourceeye"
/usr/sbin/useradd -r -m -c "sourceeye user" sourceeye -g sourceeye 2> /dev/null || :