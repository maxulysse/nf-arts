# Use the Gradle wrapper by default; override with e.g. `make GRADLE=gradle ...`
# or `export GRADLE=gradle` (useful in pixi/conda environments).
GRADLE ?= ./gradlew

# Source local, untracked settings into the environment of the command that follows.
# Copy .env.example to .env and fill it in.
LOAD_ENV = set -a; [ -f .env ] && . ./.env; set +a;

# Build the plugin
assemble:
	$(GRADLE) assemble

clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	$(GRADLE) clean

# Run plugin unit tests
test:
	$(GRADLE) test

# Install the plugin into local nextflow plugins dir
install:
	$(GRADLE) install

# Publish the plugin
release:
	$(LOAD_ENV) $(GRADLE) releasePlugin

# Local plugin repository, so `nextflow plugin nf-arts:render` resolves this build.
# Then: export NXF_PLUGINS_TEST_REPOSITORY="file://$(CURDIR)/build/plugin-repo/plugins.json"
VERSION = $(shell sed -n "s/^version = '\(.*\)'/\1/p" build.gradle)
dev-repo: assemble
	mkdir -p build/plugin-repo
	ZIP="$(CURDIR)/build/distributions/nf-arts-$(VERSION).zip"; \
	SHA=$$(sha512sum "$$ZIP" | cut -d' ' -f1); \
	printf '[{"id": "nf-arts", "releases": [{"version": "$(VERSION)", "date": "1970-01-01T00:00:00.000Z", "requires": ">=25.10.0", "url": "file://%s", "sha512sum": "%s"}]}]\n' "$$ZIP" "$$SHA" > build/plugin-repo/plugins.json
