/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gervill.com.sun.media.sound;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import gervill.javax.sound.midi.InvalidMidiDataException;
import gervill.javax.sound.midi.MetaMessage;
import gervill.javax.sound.midi.MidiEvent;
import gervill.javax.sound.midi.MidiMessage;
import gervill.javax.sound.midi.SysexMessage;
import gervill.javax.sound.midi.Track;

//=============================================================================================================

/**
 * State variables during parsing of a MIDI file.
 */
final class SMFParser {
    private static final int MTrk_MAGIC = 0x4d54726b;  // 'MTrk'

    // set to true to not allow corrupt MIDI files tombe loaded
    private static final boolean STRICT_PARSER = false;

    private static final boolean DEBUG = false;

    int tracks;                       // number of tracks
    DataInputStream stream;   // the stream to read from

    private int trackLength = 0;  // remaining length in track
    private byte[] trackData = null;
    private int pos = 0;

    SMFParser() {
    }

    private int readUnsigned() throws IOException {
        return trackData[pos++] & 0xFF;
    }

    private void read(byte[] data) throws IOException {
        System.arraycopy(trackData, pos, data, 0, data.length);
        pos += data.length;
    }

    private long readVarInt() throws IOException {
        long value = 0; // the variable-lengh int value
        int currentByte = 0;
        do {
            currentByte = trackData[pos++] & 0xFF;
            value = (value << 7) + (currentByte & 0x7F);
        } while ((currentByte & 0x80) != 0);
        return value;
    }

    private int readIntFromStream() throws IOException {
        try {
            return stream.readInt();
        } catch (EOFException eof) {
            throw new EOFException("invalid MIDI file");
        }
    }

    boolean nextTrack() throws IOException, InvalidMidiDataException {
        int magic;
        trackLength = 0;
        do {
            // $$fb 2003-08-20: fix for 4910986: MIDI file parser breaks up on http connection
            if (stream.skipBytes(trackLength) != trackLength) {
                if (!STRICT_PARSER) {
                    return false;
                }
                throw new EOFException("invalid MIDI file");
            }
            magic = readIntFromStream();
            trackLength = readIntFromStream();
        } while (magic != MTrk_MAGIC);
        if (!STRICT_PARSER) {
            if (trackLength < 0) {
                return false;
            }
        }
        // now read track in a byte array
        try {
            trackData = new byte[trackLength];
        } catch (final OutOfMemoryError oom) {
            throw new IOException("Track length too big", oom);
        }
        try {
            // $$fb 2003-08-20: fix for 4910986: MIDI file parser breaks up on http connection
            stream.readFully(trackData);
        } catch (EOFException eof) {
            if (!STRICT_PARSER) {
                return false;
            }
            throw new EOFException("invalid MIDI file");
        }
        pos = 0;
        return true;
    }

    private boolean trackFinished() {
        return pos >= trackLength;
    }

    void readTrack(Track track) throws IOException, InvalidMidiDataException {
        try {
            // reset current tick to 0
            long tick = 0;

            // reset current status byte to 0 (invalid value).
            // this should cause us to throw an InvalidMidiDataException if we don't
            // get a valid status byte from the beginning of the track.
            int status = 0;
            boolean endOfTrackFound = false;

            while (!trackFinished() && !endOfTrackFound) {
                MidiMessage message;

                int data1 = -1;         // initialize to invalid value
                int data2 = 0;

                // each event has a tick delay and then the event data.

                // first read the delay (a variable-length int) and update our tick value
                tick += readVarInt();

                // check for new status
                int byteValue = readUnsigned();

                if (byteValue >= 0x80) {
                    status = byteValue;
                } else {
                    data1 = byteValue;
                }

                switch (status & 0xF0) {
                case 0x80:
                case 0x90:
                case 0xA0:
                case 0xB0:
                case 0xE0:
                    // two data bytes
                    if (data1 == -1) {
                        data1 = readUnsigned();
                    }
                    data2 = readUnsigned();
                    message = new FastShortMessage(status | (data1 << 8) | (data2 << 16));
                    break;
                case 0xC0:
                case 0xD0:
                    // one data byte
                    if (data1 == -1) {
                        data1 = readUnsigned();
                    }
                    message = new FastShortMessage(status | (data1 << 8));
                    break;
                case 0xF0:
                    // sys-ex or meta
                    switch(status) {
                    case 0xF0:
                    case 0xF7:
                        // sys ex
                        int sysexLength = (int) readVarInt();
                        byte[] sysexData = new byte[sysexLength];
                        read(sysexData);

                        SysexMessage sysexMessage = new SysexMessage();
                        sysexMessage.setMessage(status, sysexData, sysexLength);
                        message = sysexMessage;
                        break;

                    case 0xFF:
                        // meta
                        int metaType = readUnsigned();
                        int metaLength = (int) readVarInt();
                        final byte[] metaData;
                        try {
                            metaData = new byte[metaLength];
                        } catch (final OutOfMemoryError oom) {
                            throw new IOException("Meta length too big", oom);
                        }

                        read(metaData);

                        MetaMessage metaMessage = new MetaMessage();
                        metaMessage.setMessage(metaType, metaData, metaLength);
                        message = metaMessage;
                        if (metaType == 0x2F) {
                            // end of track means it!
                            endOfTrackFound = true;
                        }
                        break;
                    default:
                        throw new InvalidMidiDataException("Invalid status byte: " + status);
                    } // switch sys-ex or meta
                    break;
                default:
                    throw new InvalidMidiDataException("Invalid status byte: " + status);
                } // switch
                track.add(new MidiEvent(message, tick));
            } // while
        } catch (ArrayIndexOutOfBoundsException e) {
            if (DEBUG) e.printStackTrace();
            // fix for 4834374
            throw new EOFException("invalid MIDI file");
        }
    }
}
