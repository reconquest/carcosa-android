DEBUG_ANDROID_PACKAGE=io.reconquest.carcosa.debug

_MAKE=$(MAKE) \
	  --no-print-directory \
	  -s

_ADB=adb -s $(shell adb devices -l | tail -n+2 | cut -f1 -d' ' | head -n1)

ifdef FASTBUILD
GRADLE_BUILD_FLAGS = -x lint -x lintVitalRelease
else
GRADLE_BUILD_FLAGS =
endif

RELEASE_VERSION=$(shell printf "%s.%s" \
	$$(git rev-list --count HEAD) \
	$$(git rev-parse --short HEAD) \
)
RELEASE_NOTE=$(shell git show -s --format=%s)

ifdef EMULATOR
so:
	@rm -rf src/main/jniLibs/$*
	@$(_MAKE) GOARCH=amd64 CCARCH=x86_64 lib-x86_64
else
so:
	# add other archs there
	@rm -rf src/main/jniLibs/$*
	@$(_MAKE) GOARCH=arm64 CCARCH=aarch64 lib-arm64-v8a
endif

lib-%:
	@$(_MAKE) src/main/jniLibs/$*/libcarcosa.so

run: install
	$(_ADB) shell am start -n $(DEBUG_ANDROID_PACKAGE)/.LoginActivity

install: build/debug.apk
	$(_ADB) install -r build/debug.apk

%/keystore:
	@echo :: initializing $*/keystore using $*/vars
	export $$(cat $*/vars) && keytool -genkeypair \
		-alias $$KEYSTORE_ALIAS \
		-validity $$KEYSTORE_VALIDITY \
		-keystore $@ \
		-keyalg $$KEYSTORE_ALGORITHM \
		-keysize $$KEYSTORE_SIZE \
		-storepass $$KEYSTORE_PASSWORD \
		-keypass $$KEYSTORE_PASSWORD \
		-dname $$KEYSTORE_DN \
		-deststoretype pkcs12

src/main/jniLibs/%/libcarcosa.so:
	@echo :: compiling carcosa shared lib CCARCH=$(CCARCH) GOARCH=$(GOARCH)
	@CGO_ENABLED=1 GO111MODULE=on \
		GOOS=android \
		CC=$(CCARCH)-linux-android21-clang \
		CXX=$(CCARCH)-linux-android21-clang++ \
		CCARCH=$(CCARCH) \
		GOARCH=$(GOARCH) \
		 go build \
		 	-o=$@ \
			-buildmode=c-shared ./lib

build/debug.apk: src/debug/keystore java

java: src/debug/keystore
	gradle assembleDebug $(GRADLE_BUILD_FLAGS)
	@mv build/outputs/apk/debug/carcosa-android-debug.apk build/debug.apk

clean:
	rm -rf build
	go clean -cache

eclipse:
	gradle eclipse
	gradle pom

eclipse-clean:
	rm -rf .classpath .project .settings/

build/release.apk:
	gradle assembleRelease
	mv build/outputs/apk/release/carcosa-android-release.apk build/release.apk

release: build/release.apk
	$(warning Make sure changes commited to collect release notes)
	export $$(cat src/release/vars) && firebase \
		appdistribution:distribute build/release.apk \
		--release-notes "$(RELEASE_VERSION): $(RELEASE_NOTE)" \
		--app $$APP_ID
