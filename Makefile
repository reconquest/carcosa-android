DEBUG_ANDROID_PACKAGE=io.reconquest.carcosa.debug
ANDROID_PACKAGE=io.reconquest.carcosa

export JAVA_HOME ?= /usr/lib/jvm/java-17-openjdk
export ANDROID_HOME ?= /opt/android-sdk

NDK_VERSION ?= 28.2.13676358
NDK_TOOLCHAIN=$(ANDROID_HOME)/ndk/$(NDK_VERSION)/toolchains/llvm/prebuilt/linux-x86_64/bin
export PATH := $(NDK_TOOLCHAIN):$(JAVA_HOME)/bin:$(PATH)

_MAKE=$(MAKE) \
	  --no-print-directory \
	  -s

_ADB=adb -s $(shell adb devices -l | tail -n+2 | cut -f1 -d' ' | head -n1)
_LIB_SRC=$(shell find lib -type f)

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

so:
	@$(_MAKE) GOARCH=amd64 CCARCH=x86_64 lib-x86_64
	@$(_MAKE) GOARCH=arm64 CCARCH=aarch64 lib-arm64-v8a

lib-%:
	@$(_MAKE) src/main/jniLibs/$*/libcarcosa.so

src/main/jniLibs/%/libcarcosa.so: $(_LIB_SRC)
	@echo :: compiling carcosa shared lib CCARCH=$(CCARCH) GOARCH=$(GOARCH)
	@CGO_ENABLED=1 GO111MODULE=on \
		GOOS=android \
		CC=$(CCARCH)-linux-android21-clang \
		CXX=$(CCARCH)-linux-android21-clang++ \
		CCARCH=$(CCARCH) \
		GOARCH=$(GOARCH) \
		 go build \
		 	-o=$@ \
			-ldflags='-extldflags=-Wl,-z,max-page-size=16384' \
			-buildmode=c-shared ./lib

debug@run: debug@install
	$(_ADB) shell am start -n $(DEBUG_ANDROID_PACKAGE)/$(ANDROID_PACKAGE).LoginActivity

debug@install: debug@apk
	$(_ADB) install -r build/debug.apk

release@run: release@install
	$(_ADB) shell am start -n $(ANDROID_PACKAGE)/.LoginActivity

release@install: release@apk
	$(_ADB) install -r build/release.apk

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

debug@apk: so src/debug/keystore
	gradle assembleDebug $(GRADLE_BUILD_FLAGS)
	@mv build/outputs/apk/debug/carcosa-android-debug.apk build/debug.apk

debug: debug@run

release@apk: so src/release/keystore
	gradle assembleRelease
	@mv build/outputs/apk/release/carcosa-android-release.apk build/release.apk

release: release@apk
	$(warning Make sure changes commited to collect release notes)
	export $$(cat src/release/vars) && firebase \
		appdistribution:distribute build/release.apk \
		--release-notes "$(RELEASE_VERSION): $(RELEASE_NOTE)" \
		--app $$APP_ID \
		--groups beta

clean:
	rm -rf build
	go clean -cache

eclipse:
	gradle eclipse
	gradle pom

eclipse@clean: clean
	rm -rf .classpath .project .settings/
