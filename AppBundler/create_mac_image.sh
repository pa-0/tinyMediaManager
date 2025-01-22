#!/bin/sh

mkdir -p dist

###############################
# x64
###############################
mkdir macos_dmg_x64
cd macos_dmg_x64
unzip -q -X ../target/tinyMediaManager-*macos-x86_64.zip -d tinyMediaManager
VERSION=$(grep 'human.version' tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/version | cut -d'=' -f2)

touch tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/.userdir

# sign
echo "signing flatlaf dylib"
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/flatlaf-macos-*.dylib

echo "signing JNA dylibs"
unzip tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/jna.jar -d jna
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" jna/com/sun/jna/darwin-aarch64/libjnidispatch.jnilib
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" jna/com/sun/jna/darwin-x86-64/libjnidispatch.jnilib
cd jna
zip -r ../jna.jar *
cd ..
cp jna.jar tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/jna.jar

echo "signing app"
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app

# prepare dmg
cp ../AppBundler/macos/DS_Store tinyMediaManager/.DS_Store
../AppBundler/macos/bin/create-dmg \
--background ../AppBundler/macos/background.png \
--volname "tinyMediaManager" \
--window-pos 200 120 \
--window-size 660 400 \
--icon-size 100 \
--icon "tinyMediaManager.app" 160 180 \
--hide-extension "tinyMediaManager.app" \
--app-drop-link 500 175 \
--skip-jenkins \
--format UDBZ \
tinyMediaManager.dmg \
tinyMediaManager

# sign dmg
echo "signing dmg"
codesign --force --options=runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager.dmg

# notarize dmg
echo "notarizing"
xcrun notarytool submit tinyMediaManager.dmg --apple-id "${MAC_APPLE_ID}" --team-id "${MAC_TEAM_ID}" --password "${MAC_NOTARIZE_PASSWORD}"
#xcrun stapler staple tinyMediaManager.dmg

# copy to dist
ditto tinyMediaManager.dmg ../dist/tinyMediaManager-$VERSION-macos-x86_64.dmg

# cleanup
cd ..
rm -rf macos_dmg_x64


###############################
# arm64
###############################
mkdir macos_dmg_aarch64
cd macos_dmg_aarch64
unzip -q -X ../target/tinyMediaManager-*macos-aarch64.zip -d tinyMediaManager
VERSION=$(grep 'human.version' tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/version | cut -d'=' -f2)

touch tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/.userdir

# sign
echo "signing flatlaf dylib"
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/flatlaf-macos-*.dylib

echo "signing JNA dylibs"
unzip tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/jna.jar -d jna
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" jna/com/sun/jna/darwin-aarch64/libjnidispatch.jnilib
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" jna/com/sun/jna/darwin-x86-64/libjnidispatch.jnilib
cd jna
zip -r ../jna.jar *
cd ..
cp jna.jar tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/lib/jna.jar

echo "signing app"
codesign --force --options=runtime --deep --timestamp --entitlements ../AppBundler/macos/hardened_runtime_entitlements.plist --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app

# prepare dmg
cp ../AppBundler/macos/DS_Store tinyMediaManager/.DS_Store
../AppBundler/macos/bin/create-dmg \
--background ../AppBundler/macos/background.png \
--volname "tinyMediaManager" \
--window-pos 200 120 \
--window-size 660 400 \
--icon-size 100 \
--icon "tinyMediaManager.app" 160 180 \
--hide-extension "tinyMediaManager.app" \
--app-drop-link 500 175 \
--skip-jenkins \
--format ULMO \
tinyMediaManager.dmg \
tinyMediaManager

# sign dmg
echo "signing dmg"
codesign --force --options=runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager.dmg

# notarize dmg
echo "notarizing"
xcrun notarytool submit tinyMediaManager.dmg --apple-id "${MAC_APPLE_ID}" --team-id "${MAC_TEAM_ID}" --password "${MAC_NOTARIZE_PASSWORD}"
#xcrun stapler staple tinyMediaManager.dmg

# copy to dist
ditto tinyMediaManager.dmg ../dist/tinyMediaManager-$VERSION-macos-aarch64.dmg

# cleanup
cd ..
rm -rf macos_dmg_aarch64