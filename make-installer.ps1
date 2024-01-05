$DIR='jre'
Remove-Item $DIR -Recurse -ErrorAction Ignore
jlink `
--no-header-files `
--no-man-pages `
--compress=2 `
--strip-debug `
--add-modules java.base,java.desktop,java.net.http,java.naming,jdk.crypto.ec `
--output jre
mvn clean package
jpackage `
--app-version "1.0" `
--input 'Executable\target\modules' `
--dest "release" `
--name "JamAlong" `
--type "exe" `
--runtime-image 'jre' `
--main-jar 'JamAlong.jar' `
--main-class 'com.hawolt.Main' `
--win-shortcut `
--win-dir-chooser `
--icon 'Executable\src\main\resources\Jamalong.ico'
