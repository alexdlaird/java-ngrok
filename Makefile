.PHONY: all build install clean init-test-resources prune-test-resources test docs check local validate-release test-downstream upload

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

all: test

install: local

build:
	$(GRADLE_BIN) assemble

clean:
	$(GRADLE_BIN) clean
	@rm -rf java-ngrok-example-dropwizard

init-test-resources:
	python -m pip install pyngrok
	python scripts/init_test_resources.py

prune-test-resources:
	python -m pip install pyngrok
	python scripts/prune_test_resources.py

test:
	$(GRADLE_BIN) test

docs:
	$(GRADLE_BIN) javadoc

check:
	$(GRADLE_BIN) checkstyleMain spotbugsMain

local:
	$(GRADLE_BIN) publishToMavenLocal

validate-release:
	@if [[ "${VERSION}" == "" ]]; then echo "VERSION is not set" & exit 1 ; fi

	@if [[ $$(grep "version \"${VERSION}\"" build.gradle) == "" ]] ; then echo "Version not bumped in build.gradle" & exit 1 ; fi

test-downstream:
	@( \
		VERSION=$(shell ./gradlew -q printVersion); \
		git clone --branch java8 https://github.com/alexdlaird/java-ngrok-example-dropwizard.git; \
		( make local ) || exit $$?; \
		mvn -f java-ngrok-example-dropwizard/pom.xml versions:set-property -Dproperty=java-ngrok.version -DnewVersion=$$VERSION; \
		( make -C java-ngrok-example-dropwizard build ) || exit $$?; \
		( make -C java-ngrok-example-dropwizard test ) || exit $$?; \
		rm -rf java-ngrok-example-dropwizard; \
	)

upload:
	$(GRADLE_BIN) publish jreleaserFullRelease
