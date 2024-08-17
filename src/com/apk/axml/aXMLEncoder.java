package com.apk.axml;

import android.content.Context;

import com.apk.axml.utils.Chunk;
import com.apk.axml.utils.IntWriter;
import com.apk.axml.utils.StringPoolChunk;
import com.apk.axml.utils.TagChunk;
import com.apk.axml.utils.XmlChunk;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 22, 2023
 * Based on the original work of @hzw1199 (https://github.com/hzw1199/xml2axml/)
 * & @WindySha (https://github.com/WindySha/Xpatch)
 */
public class aXMLEncoder {

    public static class Config {
        public static StringPoolChunk.Encoding encoding = StringPoolChunk.Encoding.UNICODE;
        public static int defaultReferenceRadix = 16;
    }
    
    public void encodeFile(Context context, FileInputStream inputStream, FileOutputStream outputStream) throws XmlPullParserException, IOException {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser p = f.newPullParser();
        p.setInput(inputStream, "UTF-8");
        encode(context, p, outputStream);
    }
    
    public void encodeString(Context context, String xml, FileOutputStream outputStream) throws XmlPullParserException, IOException {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser p = f.newPullParser();
        p.setInput(new StringReader(xml));
        encode(context, p, outputStream);
    }

    private static void encode(Context context, XmlPullParser p, FileOutputStream outputStream) throws XmlPullParserException, IOException {
        XmlChunk chunk = new XmlChunk(context);
        TagChunk current = null;
        for (int i = p.getEventType(); i != XmlPullParser.END_DOCUMENT; i = p.next()) {
            switch (i) {
                case XmlPullParser.START_TAG:
                    current = new TagChunk(current == null ? chunk : current, p);
                    break;
                case XmlPullParser.END_TAG:
                    Chunk c = current.getParent();
                    current = c instanceof TagChunk ? (TagChunk) c : null;
                    break;
                default:
                    break;
            }
        }
        IntWriter w = new IntWriter(outputStream);
        chunk.write(w);
        w.close();
        outputStream.close();
    }

}
