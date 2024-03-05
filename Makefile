.PHONY: all build install clean test docs check local validate-release test-downstream upload

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
	$(GRADLE_BIN) build -x test

clean:
	$(GRADLE_BIN) clean
	@rm -rf java-ngrok-example-dropwizard

test:
	$(GRADLE_BIN) test

docs:
	$(GRADLE_BIN) javadoc

check:

local:
	$(GRADLE_BIN) publishToMavenLocal

validate-release:
	@if [[ "${VERSION}" == "" ]]; then echo "VERSION is not set" & exit 1 ; fi

	@if [[ $$(grep "version \"${VERSION}\"" build.gradle) == "" ]] ; then echo "Version not bumped in build.gradle" & exit 1 ; fi
	@if [[ $$(grep "VERSION = \"${VERSION}\"" src/main/java/com/github/alexdlaird/ngrok/NgrokClient.java) == "" ]] ; then echo "Version not bumped in NgrokClient.java" & exit 1 ; fi

test-downstream:
	@if [[ "${VERSION}" == "" ]]; then echo "VERSION is not set" & exit 1 ; fi
	@( \
		git clone https://github.com/alexdlaird/java-ngrok-example-dropwizard.git; \
		( make local ) || exit $$?; \
		mvn -f java-ngrok-example-dropwizard/pom.xml versions:set-property -Dproperty=java-ngrok.version -DnewVersion=${VERSION}; \
		( make -C java-ngrok-example-dropwizard build ) || exit $$?; \
		( make -C java-ngrok-example-dropwizard test ) || exit $$?; \
		rm -rf java-ngrok-example-dropwizard; \
	)

upload:
	$(GRADLE_BIN) publishToSonatype closeAndReleaseSonatypeStagingRepository
