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

SUPPORT=android.support.v7.appcompat:appcompat-v7:25.3.1 \
		android.support.design:design:25.3.1

define support
	$(call support_extract,$(word 2,$(subst :, ,$(1))),$(word 3,$(subst :, ,$(1))))
endef

define support_deps
	xml2 < $(ANDROID_M2REPO)/com/android/support/$(1)/$(2)/$(1)-$(2).pom \
		| grep -Po 'dependency/(groupId|artifactId|version)=\K.*' \
		| paste -sd'::\n'
endef

define support_extract
	$(foreach dep,$(shell $(call support_deps,$(1),$(2))),$(call support,$(dep)))

	@artifact=$(ANDROID_M2REPO)/com/android/support/$(1)/$(2)/$(1)-$(2); \
	[ ! -d $(OUT_DIR)/support/$(1) ] \
		&& echo :: collecting android support lib $(1) $(2) \
		&& ( \
			mkdir -p $(OUT_DIR)/support/$(1) && \
			[ -f $$artifact.aar ] \
				&& unzip -qq -o -d $(OUT_DIR)/support/$(1) $$artifact.aar \
				&& mkdir -p $(OUT_DIR)/support/$(1)/java \
				|| cp $$artifact.jar $(OUT_DIR)/support/$(1)/classes.jar && \
			unzip -qq -o -d $(OUT_DIR)/support/$(1)/java/ \
				$(OUT_DIR)/support/$(1)/classes.jar \
		) || :
endef

_BUILD_TOOLS=$(ANDROID_SDK_PATH)/build-tools/$(ANDROID_SDK_VERSION)
_ANDROID_JAR_PATH=$(ANDROID_SDK_PATH)/platforms/android-$(ANDROID_TOOLCHAIN_VERSION)/android.jar
_ANDROID_TOOLCHAIN_PATH=$(ANDROID_NDK_PATH)/toolchains/llvm/prebuilt/linux-x86_64/bin

# Tools helpers.
_JAVAC=javac \
	-classpath src:$(shell find $(OUT_DIR)/support -name classes.jar | paste -sd:) \
	-bootclasspath $(_ANDROID_JAR_PATH) \
	-d $(OUT_DIR)/obj

_MAKE=$(MAKE) \
	  --no-print-directory \
	  -s

_AAPT_PACKAGE=$(_BUILD_TOOLS)/aapt package \
	-f \
	-m \
	--auto-add-overlay \
	-I $(_ANDROID_JAR_PATH)

_AAPT_PACKAGE_RES=$(_AAPT_PACKAGE) \
	-S src/main/res \
	$(foreach lib,$(SUPPORT),-S $(OUT_DIR)/support/$(word 2,$(subst :, ,$(lib)))/res) \
	--extra-packages \
	$(subst $() $(),:,$(foreach lib,$(SUPPORT),$(word 1,$(subst :, ,$(lib)))))

_JAVA_SRC=$(shell find src -name '*.java')

#_ADB=adb -s QMU7N17B03000481
_ADB=adb -s emulator-5554

so:
	#@$(_MAKE) GOARCH=arm64 CCARCH=aarch64 lib-arm64-v8a
	@$(_MAKE) GOARCH=amd64 CCARCH=x86_64 lib-x86_64

lib-%:
	@rm -rf $(OUT_DIR)/lib/$*/libcarcosa.so
	@$(_MAKE) $(OUT_DIR)/lib/$*/libcarcosa.so

run: install
	$(_ADB) shell am start -n $(ANDROID_PACKAGE)/.MainActivity

install: $(OUT_DIR)/app.apk
	$(_ADB) install -r --fastdeploy $(OUT_DIR)/app.apk

# Initialize keystore to sign APK.
.keystore:
	@echo :: initializing keystore
	@keytool -genkeypair \
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
	@echo :: packaging project resources
	@$(_AAPT_PACKAGE_RES) \
		-M src/main/AndroidManifest.xml \
		-J src/main/java

# Compile Java code.
compile: $(_JAVA_SRC)
	@mkdir -p $(OUT_DIR)/obj

	@echo :: compiling project java files
	@$(_JAVAC) $(_JAVA_SRC)

# Convert compiled Java code into DEX file (required by Android).
dex:
	@echo :: dexing java compiled code
	@$(_BUILD_TOOLS)/dx \
		--dex \
		--incremental \
		--output $(OUT_DIR)/classes.dex \
		$(OUT_DIR)/obj \
		$(OUT_DIR)/support/*/classes.jar

clean:
	git clean -ffdx

$(OUT_DIR)/lib/%/libcarcosa.so:
	@echo :: compiling carcosa shared lib CCARCH=$(CCARCH) GOARCH=$(GOARCH)
	@CGO_ENABLED=1 GO111MODULE=on \
		GOOS=android \
		 CC=$(CCARCH)-linux-android21-clang \
		 CXX=$(CCARCH)-linux-android21-clang++ \
		 go build \
		 	-o=$@ \
			-buildmode=c-shared ./lib

# Unpack support libraries.
$(OUT_DIR)/support:
	$(foreach dep,$(SUPPORT),$(call support,$(dep)))

# Package everything into unaligned APK file.
$(OUT_DIR)/app.unaligned.apk: resources so compile dex
	@echo :: packaging unaligned apk
	@$(_AAPT_PACKAGE_RES) \
		-M src/main/AndroidManifest.xml \
		-F $(OUT_DIR)/app.unaligned.base.apk
	@cd $(OUT_DIR) && $(_BUILD_TOOLS)/aapt add \
		app.unaligned.base.apk \
		lib/*/*
	@cp $(OUT_DIR)/app.unaligned.base.apk $(OUT_DIR)/app.unaligned.apk
	@cd $(OUT_DIR) && $(_BUILD_TOOLS)/aapt add \
		app.unaligned.apk \
		classes.dex

run+java: resources compile dex
	@echo :: packaging unaligned apk - java updates
	@$(_AAPT_PACKAGE_RES) \
		-M src/main/AndroidManifest.xml \
		-u \
		-F $(OUT_DIR)/app.unaligned.base.apk
	@cp $(OUT_DIR)/app.unaligned.base.apk $(OUT_DIR)/app.unaligned.apk
	@cd $(OUT_DIR) && $(_BUILD_TOOLS)/aapt add \
		app.unaligned.apk \
		classes.dex
	@$(_BUILD_TOOLS)/apksigner sign \
		--ks .keystore \
		--ks-pass pass:$(KEYS_PASS) \
		$(OUT_DIR)/app.unaligned.apk
	$(_ADB) install -r --fastdeploy $(OUT_DIR)/app.unaligned.apk
	$(_ADB) shell am start -n $(ANDROID_PACKAGE)/.MainActivity

# Align unaligned APK file and sign it using keystore.
$(OUT_DIR)/app.apk: .keystore $(OUT_DIR)/app.unaligned.apk
	@echo :: packaging final apk
	@$(_BUILD_TOOLS)/zipalign \
		-f 4 \
		$(OUT_DIR)/app.unaligned.apk \
		$(OUT_DIR)/app.apk

	@echo :: signing final apk
	@$(_BUILD_TOOLS)/apksigner sign \
		--ks .keystore \
		--ks-pass pass:$(KEYS_PASS) \
		$(OUT_DIR)/app.apk
