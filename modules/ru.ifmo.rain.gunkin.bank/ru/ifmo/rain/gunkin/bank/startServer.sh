#!/bin/bash
export CLASSPATH=../../../../..

javac -cp ../../../../.. Server.java

rmiregistry &
sleep 1
java ru.ifmo.rain.gunkin.bank.Server
