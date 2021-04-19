.PHONY: all build install clean test local upload

SHELL := /usr/bin/env bash

all: build

install: local

build:
	./gradlew build

clean:
	./gradlew clean

test:
	./gradlew test

local:
	./gradlew publishToMavenLocal

upload:
	./gradlew publish