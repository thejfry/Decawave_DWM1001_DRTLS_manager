#!/bin/bash
#set -x

TOOLS_DIR="`dirname $0`"
TOOLS_DIR=`readlink -f $TOOLS_DIR`

PS_PATTERN='decawave\.argomanager'
LOG_PATTERN="ARGO"

if  [ $# -gt 0 ]; then
	ARG="-s $1"
	echo "adding -s $1 to args..."
else
    DEVICE_COUNT=`adb devices |grep -v '^$\|List of' |wc -l`
    if [ $DEVICE_COUNT -gt 1 ]; then
        echo "choose device:"
        adb devices |grep -v '^$\|List of'
        exit
    fi 
fi

PID=""
COUNTER=10
while true; do
	PID=`adb $ARG shell ps | grep $PS_PATTERN | awk -- '{ print $2 }'`
	if [ -n "$PID" ]; then
        echo "found process, PID: $PID"
		break;
	fi
	echo waiting for app $PS_PATTERN $COUNTER
	sleep 1
	if [ $COUNTER -eq 1 ]; then
		echo "wait timed out"
		PID="very-improbable-pattern-777586648"
		break;
	fi
	let COUNTER--
done

#adb $ARG logcat | grep -v '\(nvos_linux_stub\|InputDispatcher\)' --line-buffered | grep -i "\\(Backup\\|$PID\\|$LOG_PATTERN\\)" --line-buffered | grcat grcat.conf
adb $ARG logcat | grep -a -v '\(Sherlock\|Watson\|EGL_emulation\|GC_CONCURRENT\|CONCURRENT_GC\|nativeGetEnabledTags\|nvos_linux_stub\|InputDispatcher\)' --line-buffered | grep -a "\\($PID\\|AndroidRuntime\\|$LOG_PATTERN\\)" --line-buffered | grcat $TOOLS_DIR/grcat.conf
