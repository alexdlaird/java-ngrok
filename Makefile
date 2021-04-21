.PHONY: all build install clean test docs local validate-release upload

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

docs:
	$(GRADLE_BIN) javadoc

local:
	$(GRADLE_BIN) publishToMavenLocal

validate-release:
	@if [[ "${VERSION}" == "" ]]; then echo "VERSION is not set" & exit 1 ; fi

	@if [[ $$(grep "version '${VERSION}'" build.gradle) == "" ]] ; then echo "Version not bumped in build.gradle" & exit 1 ; fi

upload:
	$(GRADLE_BIN) publish
