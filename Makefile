# To work correctly, resulting APK file should have following files:
# * classes.dex — it should contain compiled Java code.
# * lib/*/libcarcosa.so — required by android runtime.
#
# Then we pack everything into APK making sure that JNI `*.so` files are stored
# in `lib/<architecture>` directories.

# Android SDK & NDK environment settings. Modify according to SDK & NDK version
# installed in your system.
ANDROID_SDK_VERSION=28.0.3
ANDROID_SDK_PATH=/opt/android-sdk
ANDROID_NDK_PATH=/opt/android-ndk
ANDROID_TOOLCHAIN_VERSION=29

# Target package name.
ANDROID_PACKAGE=io.reconquest.carcosa

# Settings for keystore which is used to sign APK.
KEYS_DN=DC=io,CN=reconquest
KEYS_PASS=123456
KEYS_VALIDITY=365
KEYS_ALGORITHM=RSA
KEYS_SIZE=2048

# Target build dir. Resulting APK file will be available here.
OUT_DIR=out

_BUILD_TOOLS=$(ANDROID_SDK_PATH)/build-tools/$(ANDROID_SDK_VERSION)
_ANDROID_JAR_PATH=$(ANDROID_SDK_PATH)/platforms/android-$(ANDROID_TOOLCHAIN_VERSION)/android.jar
_ANDROID_TOOLCHAIN_PATH=$(ANDROID_NDK_PATH)/toolchains/llvm/prebuilt/linux-x86_64/bin
_JAVA_SRC=$(shell find java/src -name '*.java')

_JAVAC=javac -classpath src -bootclasspath $(_ANDROID_JAR_PATH)
_MAKE=$(MAKE) --no-print-directory

#_ARCHS=$(wildcard $(_ANDROID_TOOLCHAIN_PATH)/*-linux-android$(ANDROID_TOOLCHAIN_VERSION)-clang)

lib: lib-aarch64

lib-aarch64:
	@rm -rf $(OUT_DIR)/lib
	@$(_MAKE) GOARCH=arm64 CCARCH=aarch64 $(OUT_DIR)/lib/arm64-v8a/libcarcosa.so

$(OUT_DIR)/lib/%/libcarcosa.so:
	CGO_ENABLED=1 \
		GOOS=android \
		 CC=$(CCARCH)-linux-android21-clang \
		 CXX=$(CCARCH)-linux-android21-clang++ \
		 go build \
		 	-o=$@ \
			-buildmode=c-shared ./go

run: install
	adb shell am start -n $(ANDROID_PACKAGE)/.MainActivity

install: $(OUT_DIR)/app.apk
	adb install $(OUT_DIR)/app.apk

# Initialize keystore to sign APK.
.keystore:
	keytool -genkeypair \
		-validity $(KEYS_VALIDITY) \
		-keystore $@ \
		-keyalg $(KEYS_ALGORITHM) \
		-keysize $(KEYS_SIZE) \
		-storepass $(KEYS_PASS) \
		-keypass $(KEYS_PASS) \
		-dname $(KEYS_DN) \
		-deststoretype pkcs12

# Collect resources and generate R.java.
resources:
	$(_BUILD_TOOLS)/aapt package \
		-f \
		-m \
		-J java/src \
		-M AndroidManifest.xml \
		-S res \
		-I $(_ANDROID_JAR_PATH)

# Compile Java code.
compile: lib
	@mkdir -p $(OUT_DIR)/obj

	$(_JAVAC) \
		-d $(OUT_DIR)/obj \
		$(_JAVA_SRC)

# Convert compiled Java code into DEX file (required by Android).
dex:
	$(_BUILD_TOOLS)/dx \
		--dex \
		--output $(OUT_DIR)/classes.dex \
		$(OUT_DIR)/obj

# Package everything into unaligned APK file.
$(OUT_DIR)/app.apk.unaligned: resources compile dex
	$(_BUILD_TOOLS)/aapt package \
		-f \
		-m \
		-F $(OUT_DIR)/app.apk.unaligned \
		-M AndroidManifest.xml \
		-S res \
		-I $(_ANDROID_JAR_PATH)

	cd $(OUT_DIR) && $(_BUILD_TOOLS)/aapt add \
		app.apk.unaligned \
		classes.dex \
		lib/*/*

# Align unaligned APK file and sign it using keystore.
$(OUT_DIR)/app.apk: .keystore $(OUT_DIR)/app.apk.unaligned
	$(_BUILD_TOOLS)/zipalign \
		-f 4 \
		$(OUT_DIR)/app.apk.unaligned \
		$(OUT_DIR)/app.apk

	$(_BUILD_TOOLS)/apksigner sign \
		--ks .keystore \
		--ks-pass pass:$(KEYS_PASS) \
		$(OUT_DIR)/app.apk

clean:
	@rm -rf $(OUT_DIR)
