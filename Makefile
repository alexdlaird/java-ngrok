.PHONY: all build install clean create-test-resources delete-test-resources delete-temp-test-resources test docs check local validate-release test-downstream upload

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

create-test-resources:
	python -m pip install pyngrok
	python scripts/create_test_resources.py

delete-test-resources:
	python -m pip install pyngrok
	python scripts/delete_test_resources.py

delete-temp-test-resources:
	python -m pip install pyngrok
	python scripts/delete_test_resources.py --temp

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
		git clone https://github.com/alexdlaird/java-ngrok-example-dropwizard.git; \
		( make local ) || exit $$?; \
		mvn -f java-ngrok-example-dropwizard/pom.xml versions:set-property -Dproperty=java-ngrok.version -DnewVersion=$$VERSION; \
		( make -C java-ngrok-example-dropwizard build ) || exit $$?; \
		( make -C java-ngrok-example-dropwizard test ) || exit $$?; \
		rm -rf java-ngrok-example-dropwizard; \
	)

upload:
	$(GRADLE_BIN) publish jreleaserFullRelease
