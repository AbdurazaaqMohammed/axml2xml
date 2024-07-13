# axml2xml
A Java program to decompile and recompile Android binary XML files (AndroidManifest.xml and layout XML files)

Based on the hard work of axml2xml by [apk-editor](https://github.com/apk-editor/aXML), [codyi96](https://github.com/codyi96/xml2axml), [hzw1199](https://github.com/hzw1199/xml2axml) and [l741589](https://github.com/l741589/xml2axml) and [AXMLPrinter](https://github.com/developer-krushna/AXMLPrinter) by developer-krushna.

## Usage
```
java -jar axml2xml.jar d[ecode] input_file output_file
java -jar axml2xml.jar e[ncode] input_file output_file
```
* Optional argument: -am (For APK files, decode AndroidManifest.xml from APK file immediately instead of listing all XML files)
