#!/bin/sh
chown -R sourceeye:sourceeye /var/log/source-eye/
chown -R sourceeye:sourceeye /var/lib/source-eye/
systemctl daemon-reload