/*
 * Copyright 2013 monkeyboy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.googlecode.gwt.serialization;

import com.google.gwt.lang.LongLib;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;

import java.util.List;

/**
 * User: monkeyboy
 */
public class JsonSerializationStreamWriter extends AbstractSerializationStreamWriter {
    private static final char JS_QUOTE_CHAR = '\"';
    private static final char JS_ESCAPE_CHAR = '\\';
    private static final char NON_BREAKING_HYPHEN = '\u2011';

    private static final int NUMBER_OF_JS_ESCAPED_CHARS = 128;
    private static final char[] JS_CHARS_ESCAPED = new char[NUMBER_OF_JS_ESCAPED_CHARS];

    private static final String SEPARATOR = ",";

    private final Serializer serializer;
    private final StringBuilder encodeBuilder = new StringBuilder();

    private static final char NIBBLE_TO_HEX_CHAR[] = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    static {
        JS_CHARS_ESCAPED['\u0000'] = '0';
        JS_CHARS_ESCAPED['\b'] = 'b';
        JS_CHARS_ESCAPED['\t'] = 't';
        JS_CHARS_ESCAPED['\n'] = 'n';
        JS_CHARS_ESCAPED['\f'] = 'f';
        JS_CHARS_ESCAPED['\r'] = 'r';
        JS_CHARS_ESCAPED[JS_ESCAPE_CHAR] = JS_ESCAPE_CHAR;
        JS_CHARS_ESCAPED[JS_QUOTE_CHAR] = JS_QUOTE_CHAR;
    }

    public JsonSerializationStreamWriter(final Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public String toString() {
        // String se radi unatrag jer tako ga reader čita.
        final StringBuilder sb = new StringBuilder();
        sb.append("]");
        sb.insert(0, SEPARATOR + String.valueOf(getVersion()));
        sb.insert(0, SEPARATOR + String.valueOf(getFlags()));
        writeStringTable(sb);
        // ako je na početku zarez treba ga maknut.
        if (encodeBuilder.indexOf(SEPARATOR) == 0) {
            encodeBuilder.delete(0, 1);
        }
        sb.insert(0, "[" + encodeBuilder);
        return sb.toString();
    }

    @Override
    public void writeLong(long value) {
        encodeBuilder.insert(0, SEPARATOR + "'" + LongLib.toBase64(value) + "'");
    }

    @Override
    protected void append(String token) {
        assert (token != null);
        encodeBuilder.insert(0, SEPARATOR + token);
    }

    @Override
    protected String getObjectTypeSignature(Object instance) throws SerializationException {
        final Class<?> clazz;

        if (instance instanceof Enum<?>) {
            final Enum<?> e = (Enum<?>) instance;
            clazz = e.getDeclaringClass();
        } else {
            clazz = instance.getClass();
        }
        return serializer.getSerializationSignature(clazz);
    }

    @Override
    protected void serialize(Object instance, String typeSignature) throws SerializationException {
        serializer.serialize(this, instance, typeSignature);
    }

    private void writeStringTable(final StringBuilder builder) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        final List<String> stringTable = getStringTable();
        for (String s : stringTable) {
            //append(builder, quoteString(s));
            //sb.append("\"").append(ClientSerializationStreamWriter.quoteString(s)).append("\"").append(SEPARATOR);
            sb.append(escapeString(s)).append(SEPARATOR);
        }
        // obriši zadnji zarez.
        sb.delete(sb.length() - 1, sb.length());
        sb.append("]");
        builder.insert(0, SEPARATOR + sb.toString());
    }

    /**
     * Ovo je ukradeno iz: com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter
     */
    private static String escapeString(final String toEscape) {
        // make output big enough to escape every character (plus the quotes)
        char[] input = toEscape.toCharArray();
        CharVector charVector = new CharVector(input.length * 2 + 2, input.length);

        charVector.add(JS_QUOTE_CHAR);

        for (char c : input) {
            if (needsUnicodeEscape(c)) {
                unicodeEscape(c, charVector);
            } else {
                charVector.add(c);
            }
        }
        charVector.add(JS_QUOTE_CHAR);
        return String.valueOf(charVector.asArray(), 0, charVector.getSize());
    }

    /**
     * Ovo je ukradeno iz: com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter
     */
    private static void unicodeEscape(char ch, CharVector charVector) {
        charVector.add(JS_ESCAPE_CHAR);
        if (ch < NUMBER_OF_JS_ESCAPED_CHARS && JS_CHARS_ESCAPED[ch] != 0) {
            charVector.add(JS_CHARS_ESCAPED[ch]);
        } else if (ch < 256) {
            charVector.add('x');
            charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 4) & 0x0F]);
            charVector.add(NIBBLE_TO_HEX_CHAR[ch & 0x0F]);
        } else {
            charVector.add('u');
            charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 12) & 0x0F]);
            charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 8) & 0x0F]);
            charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 4) & 0x0F]);
            charVector.add(NIBBLE_TO_HEX_CHAR[ch & 0x0F]);
        }
    }

    /**
     * Ovo je ukradeno iz: com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter
     */
    private static boolean needsUnicodeEscape(final char ch) {
        switch (ch) {
            case ' ':
                // ASCII space gets caught in SPACE_SEPARATOR below, but does not
                // need to be escaped
                return false;
            case JS_QUOTE_CHAR:
            case JS_ESCAPE_CHAR:
                // these must be quoted or they will break the protocol
                return true;
            case NON_BREAKING_HYPHEN:
                // This can be expanded into a break followed by a hyphen
                return true;
            default:
                if (ch < ' ') {
                    // Chrome 11 mangles control characters
                    return true;
                }
                switch (getType(ch)) {
                    // Conservative
                    case COMBINING_SPACING_MARK:
                    case ENCLOSING_MARK:
                    case NON_SPACING_MARK:
                    case UNASSIGNED:
                    case PRIVATE_USE:
                    case SPACE_SEPARATOR:
                    case CONTROL:

                        // Minimal
                    case LINE_SEPARATOR:
                    case FORMAT:
                    case PARAGRAPH_SEPARATOR:
                    case SURROGATE:
                        return true;

                    default:
                        break;
                }
                break;
        }
        return false;
    }

    /**
     * Ovo je ukradeno iz: java.lang.Character
     */
    public static final int MIN_CODE_POINT = 0x000000;
    private static final int FAST_PATH_MAX = 255;

    public static final byte
            COMBINING_SPACING_MARK      = 8;
    public static final byte
            ENCLOSING_MARK              = 7;
    public static final byte
            NON_SPACING_MARK            = 6;
    public static final byte
            UNASSIGNED                  = 0;
    public static final byte
            PRIVATE_USE                 = 18;
    public static final byte
            SPACE_SEPARATOR             = 12;
    public static final byte
            CONTROL                     = 15;
    public static final byte
            LINE_SEPARATOR              = 13;
    public static final byte
            FORMAT                      = 16;
    public static final byte
            PARAGRAPH_SEPARATOR         = 14;
    public static final byte
            SURROGATE                   = 19;

    /**
     * Ovo je ukradeno iz: java.lang.Character
     */
    public static int getType(int codePoint) {
        int type = UNASSIGNED;

        if (codePoint >= MIN_CODE_POINT && codePoint <= FAST_PATH_MAX) {
            type = CharacterDataLatin1.getType(codePoint);
        } else {
            int plane = getPlane(codePoint);
            switch (plane) {
                case (0):
                    type = CharacterData00.getType(codePoint);
                    break;
                case (1):
                    type = CharacterData01.getType(codePoint);
                    break;
                case (2):
                    type = CharacterData02.getType(codePoint);
                    break;
                case (3): // Undefined
                case (4): // Undefined
                case (5): // Undefined
                case (6): // Undefined
                case (7): // Undefined
                case (8): // Undefined
                case (9): // Undefined
                case (10): // Undefined
                case (11): // Undefined
                case (12): // Undefined
                case (13): // Undefined
                    type = CharacterDataUndefined.getType(codePoint);
                    break;
                case (14):
                    type = CharacterData0E.getType(codePoint);
                    break;
                case (15): // Private Use
                case (16): // Private Use
                    type = CharacterDataPrivateUse.getType(codePoint);
                    break;
                default:
                    // the argument's plane is invalid, and thus is an invalid codepoint
                    // type remains UNASSIGNED
                    break;
            }
        }
        return type;
    }

    private static int getPlane(int ch) {
        return (ch >>> 16);
    }
}
