.PHONY: all build install clean test docs local validate-release upload

SHELL := /usr/bin/env bash
ifeq ($(OS),Windows_NT)
	ifneq (,$(findstring /cygdrive/,$(PATH)))
		GRADLE_BIN := ./gradlew
	else
		GRADLE_BIN := gradlew.bat
	endif
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

	@if [[ $$(grep "version \"${VERSION}\"" build.gradle) == "" ]] ; then echo "Version not bumped in build.gradle" & exit 1 ; fi
	@if [[ $$(grep "<version>${VERSION}</version>" README.md) == "" ]] ; then echo "Version not bumped in README.md" & exit 1 ; fi
	@if [[ $$(grep "&lt;version&gt;${VERSION}&lt;/version&gt;" src/main/java/overview.html) == "" ]] ; then echo "Version not bumped in overview.html" & exit 1 ; fi
	@if [[ $$(grep "VERSION = \"${VERSION}\"" src/main/java/com/github/alexdlaird/ngrok/NgrokClient.java) == "" ]] ; then echo "Version not bumped in NgrokClient.java" & exit 1 ; fi

upload:
	$(GRADLE_BIN) publishToSonatype closeAndReleaseSonatypeStagingRepository
