#!/bin/sh
#
# Gradle wrapper script for Unix
#

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
fi

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
