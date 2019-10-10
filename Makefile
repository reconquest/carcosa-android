ANDROID_PACKAGE=io.reconquest.carcosa

# Settings for keystore which is used to sign APK.
KEYS_DN        = DC=io,CN=reconquest
KEYS_PASS      = 123456
KEYS_VALIDITY  = 365
KEYS_ALGORITHM = RSA
KEYS_SIZE      = 2048
KEYS_ALIAS     = carcosa

_MAKE=$(MAKE) \
	  --no-print-directory \
	  -s

_ADB=adb -s $(shell adb devices -l | tail -n+2 | cut -f1 -d' ' | head -n1)


ifdef PHONE
GOARCH    = arm64
CCARCH    = aarch64
SO_TARGET = arm64-v8a
else
GOARCH    = amd64
CCARCH    = x86_64
SO_TARGET = x86_64
endif

so:
	@$(_MAKE) $(SO_TARGET)

lib-%:
	@rm -rf src/main/jniLibs/$*
	@$(_MAKE) src/main/jniLibs/$*/libcarcosa.so

run: install
	$(_ADB) shell am start -n $(ANDROID_PACKAGE)/.MainActivity

install: build/app.apk
	$(_ADB) install -r build/app.apk

.keystore:
	@echo :: initializing keystore
	@keytool -genkeypair \
		-alias $(KEYS_ALIAS) \
		-validity $(KEYS_VALIDITY) \
		-keystore $@ \
		-keyalg $(KEYS_ALGORITHM) \
		-keysize $(KEYS_SIZE) \
		-storepass $(KEYS_PASS) \
		-keypass $(KEYS_PASS) \
		-dname $(KEYS_DN) \
		-deststoretype pkcs12

src/main/jniLibs/%/libcarcosa.so:
	@echo :: compiling carcosa shared lib CCARCH=$(CCARCH) GOARCH=$(GOARCH) TARGET=$(SO_TARGET)
	@CGO_ENABLED=1 GO111MODULE=on \
		GOOS=android \
		CC=$(CCARCH)-linux-android21-clang \
		CXX=$(CCARCH)-linux-android21-clang++ \
		CCARCH=$(CCARCH) \
		GOARCH=$(GOARCH) \
		 go build \
		 	-o=$@ \
			-buildmode=c-shared ./lib

build/app.apk: .keystore src/main/jniLibs/$(SO_TARGET)/libcarcosa.so java

java: .keystore
	@TERM=xterm gradle build
	mv build/outputs/apk/debug/carcosa-android-debug.apk build/app.apk

clean:
	rm -rf build

eclipse:
	gradle eclipse
