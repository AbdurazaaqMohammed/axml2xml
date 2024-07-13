package com.apk.axml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import mt.modder.hub.axml.AXMLPrinter;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;

public class Main {
    private static boolean findText(String query, String in) {
        Pattern pattern = Pattern.compile(query);
        Matcher matcher = pattern.matcher(in);
        return matcher.find();
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {
        if (args.length < 3) showUsage();
        final String encodeOrDecode = args[0];
        final String inputPath = args[1];
        final String outputPath = args[2];
        final FileOutputStream outputStream = new FileOutputStream(outputPath);
        final OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        if (inputPath.endsWith("xml") || inputPath.endsWith("txt")) {
            if (encodeOrDecode.startsWith("e")) {
                new aXMLEncoder().encodeFile(new Context(), new FileInputStream(inputPath), outputStream);
                System.out.print("En");
            } else if (encodeOrDecode.startsWith("d")) {
                String convertedXml = new AXMLPrinter().convertXml(new FileInputStream(inputPath));
                convertedXml = checkAndReplaceUselessInfo(convertedXml);
                writer.write(convertedXml);
                writer.flush();
                System.out.print("De");
            } else {
                showUsage();
            }
        } else {
            final boolean split = inputPath.endsWith(".apks") || inputPath.endsWith(".apkm") || inputPath.endsWith(".xapk") || inputPath.endsWith(".aspk");
            if (args.length > 3) {
                if (args[3].equals("-am")) {
                    try (ZipFile apk = new ZipFile(inputPath)) {
                        String convertedXml = new AXMLPrinter().convertXml(split ?
                                getFileInputStreamFromZip(apk.getInputStream(apk.getEntry("base.apk")), "AndroidManifest.xml")
                                : apk.getInputStream(apk.getEntry("AndroidManifest.xml")));
                        convertedXml = checkAndReplaceUselessInfo(convertedXml);
                        writer.write(convertedXml);
                        writer.flush();
                        System.out.print("De");
                    }
                } else {
                    showUsage();
                }
            } else {
                printXmlFilesFromZip(new ZipInputStream(new BufferedInputStream(split ? getFileInputStreamFromZipFile(inputPath, "base.apk") : new FileInputStream(inputPath))));
                System.out.print("Enter the file you want to decode from the list above: ");
                try (Scanner scanner = new Scanner(System.in)) {
                    String fileName = scanner.nextLine();
                    try (ZipFile apk = new ZipFile(inputPath)) {
                        String convertedXml = new AXMLPrinter().convertXml(split ?
                                getFileInputStreamFromZip(apk.getInputStream(apk.getEntry("base.apk")), fileName)
                                : apk.getInputStream(apk.getEntry(fileName)));
                        if (fileName.equals("AndroidManifest.xml") && !split) {
                            convertedXml = checkAndReplaceUselessInfo(convertedXml);
                        }
                        writer.write(convertedXml);
                        writer.flush();
                        System.out.print("De");
                    }
                }
            }
        }
        System.out.println("coded to " + (outputPath.contains(File.separator) ? outputPath : new File(outputPath).getAbsolutePath()));
        writer.close();
    }

    private static void showUsage() {
        System.out.println("Usage:");
        System.out.println("java -jar aXML.jar d[ecode] input_file output_file");
        System.out.println("java -jar aXML.jar e[ncode] input_file output_file");
        System.out.println("Optional argument: -am (For APK files, decode AndroidManifest.xml from APK file immediately instead of listing all XML files)");
        System.out.println("Optional argument: -ru (Remove useless info (splits, assetPack, derived apk id). These elements can cause an \"App not installed error\" on some devices, so it is recommended to remove them from non-split APKs.)");
        System.exit(1);
    }

    private static InputStream getFileInputStreamFromZipFile(String zipFile, String filename) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry manifestEntry = zip.getEntry(filename);
            if (manifestEntry != null) return zip.getInputStream(manifestEntry);
            else try (InputStream baseApkStream = zip.getInputStream(zip.getEntry("base.apk"))) {
                return getFileInputStreamFromZip(baseApkStream, "AndroidManifest.xml");
            }
        }
    }

    private static InputStream getFileInputStreamFromZip(InputStream zipInputStream, String filename) throws IOException {
        ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(zipInputStream));
        ZipEntry entry;
        while ((entry = zipInput.getNextEntry()) != null) {
            if (entry.getName().equals(filename)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
            zipInput.closeEntry();
        }
        return null;
    }

    public static List<String> getListOfXmlFilesFromZip(ZipInputStream zipInputStream) {
        List<String> xmlFiles = new ArrayList<>();
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final String entryName = entry.getName();
                if (entryName.endsWith(".xml")) {
                    xmlFiles.add(entryName);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zipInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return xmlFiles;
    }

    public static void printXmlFilesFromZip(ZipInputStream zipInputStream) {
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final String entryName = entry.getName();
                if (entryName.endsWith(".xml")) {
                    System.out.println(entryName);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zipInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String checkAndReplaceUselessInfo(String convertedXml) {
        if (findText("isSplitRequired=\"true|plitTypes|AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id", convertedXml)) {
            System.out.println("Warning: Useless info (splits, assetPack, derived apk id) was detected in the manifest. These elements can cause an \"App not installed error\" on some devices, so it is recommended to remove them.");
            System.out.print("Do you want to remove these elements now? (y/n): ");
            try (Scanner scanner = new Scanner(System.in)) {
                return scanner.nextLine().equalsIgnoreCase("y") ? convertedXml.replaceAll(
                            "<[^>]*(AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id)[^>]*(.*\\n.*\\n.*/(?!.*(application|manifest)).*>|.*\\n.*/(?!.*(application|manifest))>|>)", "")
                            .replace("isSplitRequired=\"true", "isSplitRequired=\"false")
                            .replaceAll("(splitTypes|requiredSplitTypes)=\".*\"", "")
                            .trim() : convertedXml;
            }
        }
        return convertedXml;
    }
}
