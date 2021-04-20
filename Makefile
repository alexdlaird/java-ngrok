.PHONY: all build install clean test local upload

SHELL := /usr/bin/env bash
ifeq ($(OS),Windows_NT)
	GRADLE_BIN := gradlew.bat
else
	GRADLE_BIN := ./gradlew
endif

all: build

install: local

build:
	$(GRADLE_BIN) build

clean:
	$(GRADLE_BIN) clean

test:
	$(GRADLE_BIN) test

local:
	$(GRADLE_BIN) publishToMavenLocal

upload:
	$(GRADLE_BIN) publish