copy_src:
    rsync -av --progress --update ../wootz-browser/src/* ./src/

configure arch rebuild:
    #!/usr/bin/env sh
    if [ "{{rebuild}}" = "yes" ]; then
        echo "Do not configure on rebuild {{arch}}"
        exit 0
    fi
    mkdir -p ./src/out/Debug_{{arch}}
    cat << EOF > ./src/out/Debug_{{arch}}/args.gn
        target_os = "android"
        target_cpu = "{{arch}}"
        is_debug= false
        symbol_level = 0
        enable_nacl = false
        proprietary_codecs = true
        ffmpeg_branding = "Chrome"
        treat_warnings_as_errors=false

        is_java_debug = true
        is_component_build = true
        android_static_analysis = "off"
        cc_wrapper="env CCACHE_SLOPPINESS=time_macros ccache"
    EOF

    cd src && gn gen out/Debug_{{arch}}

build arch="x64" rebuild="yes": (configure arch rebuild)
    cd src && \
    autoninja -C out/Debug_{{arch}} chrome_public_apk -j 16

build_shell arch="x64" rebuild="yes": (configure arch rebuild)
    cd src && \
    autoninja -C out/Debug_{{arch}} chrome_shell_apk -j 24

# preparation:
# 1. on host adb tcpip 5555
# 2. forward port howt to wsl ssh -L 5555:localhost:5555 ivans@192.168.29.124
# 3. adb connect 127.0.0.1:5555

run arch="x64":
    # ANDROID_SERIAL=172.17.0.2:5555 src/out/Debug_{{arch}}/bin/chrome_public_apk run -vvv
    ANDROID_SERIAL=$DEVICE src/out/Debug_{{arch}}/bin/chrome_public_apk run # --args="--enable-logging=stderr --v=3"

install arch="x64":
    ADB_TRACE="all adb" ANDROID_SERIAL=172.17.0.2:5555 adb install -r -t src/out/Debug_{{arch}}/apks/WootzApp.apk

run_shell arch="x64":
    ANDROID_SERIAL=$DEVICE src/out/Debug_{{arch}}/bin/content_shell_apk run --args='--disable-fre' 'data:text/html;utf-8,<html>Hello World!</html>'

symbolize arch="x64":
    ANDROID_SERIAL=$DEVICE adb logcat -d | src/third_party/android_platform/development/scripts/stack --output-directory src/out/Debug_{{arch}} && adb logcat -c

bundle:
    rm -f src/out/Release_arm64/apks/WootzBundle.apks
    java -jar src/third_party/android_build_tools/bundletool/bundletool.jar build-apks \
    --bundle=src/out/Release_arm64/apks/WootzApp.aab \
    --output=src/out/Release_arm64/apks/WootzBundle.apks
    java -jar src/third_party/android_build_tools/bundletool/bundletool.jar install-apks \
    --device-id=$DEVICE \
    --apks=src/out/Release_arm64/apks/WootzBundle.apks

commit branch:
    cd ../wootz-browser && git switch -C {{branch}} && git reset --hard HEAD
    cd src && git status -s | grep ' M ' | choose 1 | xargs -I {} cp -v --parents {} ../../wootz-browser/src
    cd ../wootz-browser && git status

setup_device:
    #!/usr/bin/env bash
    set -euxo pipefail
    # DEVICE=$(docker inspect -f '{{{{range.NetworkSettings.Networks}}{{{{.IPAddress}}{{{{end}}' android-container):5555
    #DEVICE=emulator-5554
    adb connect $DEVICE
    ANDROID_SERIAL=$DEVICE adb root
    # ANDROID_SERIAL=$DEVICE adb shell pm set-install-location 1
    ANDROID_SERIAL=$DEVICE adb shell settings put global development_settings_enabled 1
    ANDROID_SERIAL=$DEVICE adb shell settings put global verifier_verify_adb_install 0
    ANDROID_SERIAL=$DEVICE adb shell settings put global art_verifier_verify_debuggable 0

emulator:
    #!/usr/bin/env bash
    cd src
    tools/android/avd/avd.py start -v --avd-config tools/android/avd/proto/generic_android33.textpb --enable-network --emulator-window --no-read-only