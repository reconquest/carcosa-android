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
ANDROID_M2REPO=$(ANDROID_SDK_PATH)/extras/android/m2repository

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

SUPPORT_AAR=appcompat-v7:25.3.1
SUPPORT_JAR=v4/android-support-v4.jar android-support-vectordrawable.jar

define support_extract
	unzip -qq -d $(OUT_DIR)/support/$(1) -o \
		$(ANDROID_M2REPO)/com/android/support/$(1)/$(2)/$(1)-$(2).aar
	unzip -qq -d $(OUT_DIR)/support/$(1)/java/ -o \
		$(OUT_DIR)/support/$(1)/classes.jar
	find $(OUT_DIR)/support/$(1)/java -name '*.java' -exec \
		$(_JAVAC) -d $(OUT_DIR)/obj {} \;
endef

_BUILD_TOOLS=$(ANDROID_SDK_PATH)/build-tools/$(ANDROID_SDK_VERSION)
_ANDROID_JAR_PATH=$(ANDROID_SDK_PATH)/platforms/android-$(ANDROID_TOOLCHAIN_VERSION)/android.jar
_ANDROID_TOOLCHAIN_PATH=$(ANDROID_NDK_PATH)/toolchains/llvm/prebuilt/linux-x86_64/bin

# Tools helpers.
_JAVAC=javac -classpath src -bootclasspath $(_ANDROID_JAR_PATH)
_MAKE=$(MAKE) --no-print-directory
_AAPT_PACKAGE=$(_BUILD_TOOLS)/aapt package \
	-f \
	-m \
	-M src/main/AndroidManifest.xml \
	-S src/main/res \
	$(foreach lib,\
		$(SUPPORT_AAR),\
		-S $(OUT_DIR)/support/$(firstword $(subst :, ,$(lib)))/res) \
	-I $(_ANDROID_JAR_PATH)

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
			-buildmode=c-shared ./pkg/lib

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
resources: $(OUT_DIR)/support
	$(_AAPT_PACKAGE) -J src/main/java

# Compile Java code.
compile: lib
	@mkdir -p $(OUT_DIR)/obj

	$(_JAVAC) \
		-d $(OUT_DIR)/obj \
		$(shell find src -name '*.java')

# Convert compiled Java code into DEX file (required by Android).
dex:
	$(_BUILD_TOOLS)/dx \
		--dex \
		--output $(OUT_DIR)/classes.dex \
		$(OUT_DIR)/obj \
		$(OUT_DIR)/support/*/classes.jar \
		$(foreach jar, \
			$(SUPPORT_JAR), \
			$(shell \
				find $(ANDROID_SDK_PATH)/extras/android/support \
					-path '*/$(jar)'))

# Unpack support libraries.
$(OUT_DIR)/support:
	@mkdir -p $(OUT_DIR)/support
	$(foreach aar, \
		$(SUPPORT_AAR), \
		$(call support_extract,$(firstword $(subst :, ,$(aar))),$(lastword $(subst :, ,$(aar)))))

# Package everything into unaligned APK file.
$(OUT_DIR)/app.apk.unaligned: resources compile dex
	$(_AAPT_PACKAGE) -F $(OUT_DIR)/app.apk.unaligned

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
	git clean -ffdx
