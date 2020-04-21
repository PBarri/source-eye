#!/bin/sh
echo "Removing user: sourceeye"
/usr/sbin/userdel -r sourceeye 2> /dev/null || :

echo "Removing group: sourceeye"
/usr/sbin/groupdel -f -r sourceeye 2> /dev/null || :