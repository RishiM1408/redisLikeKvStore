#!/bin/sh

# For kvstore services
if [ -f /app/app.jar ]; then
    # Check if Java process is running
    if ! pgrep -f "java.*app.jar" > /dev/null; then
        exit 1
    fi
    exit 0
fi

# For HAProxy
if [ -f /usr/local/etc/haproxy/haproxy.cfg ]; then
    # Check if HAProxy is running
    if ! pgrep haproxy > /dev/null; then
        exit 1
    fi
    exit 0
fi

# Default case
exit 1 