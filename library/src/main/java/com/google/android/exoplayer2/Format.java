/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Representation of a media format.
 */
public final class Format implements Parcelable {

  /**
   * Sorts {@link Format} objects in order of decreasing bandwidth.
   */
  public static final class DecreasingBandwidthComparator implements Comparator<Format> {

    @Override
    public int compare(Format a, Format b) {
      return b.bitrate - a.bitrate;
    }

  }

  public static final int NO_VALUE = -1;

  /**
   * Indicates that the track should be selected if user preferences do not state otherwise.
   */
  public static final int SELECTION_FLAG_DEFAULT = 1;

  /**
   * Indicates that the track must be displayed. Only applies to text tracks.
   */
  public static final int SELECTION_FLAG_FORCED = 2;
  /**
   * Indicates that the player may choose to play the track in absence of an explicit user
   * preference.
   */
  public static final int SELECTION_FLAG_AUTOSELECT = 4;

  /**
   * A value for {@link #subsampleOffsetUs} to indicate that subsample timestamps are relative to
   * the timestamps of their parent samples.
   */
  public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;

  /**
   * An identifier for the format, or null if unknown or not applicable.
   */
  public final String id;
  /**
   * The average bandwidth in bits per second, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int bitrate;
  /**
   * Codecs of the format as described in RFC 6381, or null if unknown or not applicable.
   */
  public final String codecs;

  // Container specific.

  /**
   * The mime type of the container, or null if unknown or not applicable.
   */
  public final String containerMimeType;

  // Elementary stream specific.

  /**
   * The mime type of the elementary stream (i.e. the individual samples), or null if unknown or not
   * applicable.
   */
  public final String sampleMimeType;
  /**
   * The maximum size of a buffer of data (typically one sample), or {@link #NO_VALUE} if unknown or
   * not applicable.
   */
  public final int maxInputSize;
  /**
   * Initialization data that must be provided to the decoder. Will not be null, but may be empty
   * if initialization data is not required.
   */
  public final List<byte[]> initializationData;
  /**
   * DRM initialization data if the stream is protected, or null otherwise.
   */
  public final DrmInitData drmInitData;

  // Video specific.

  /**
   * The width of the video in pixels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int width;
  /**
   * The height of the video in pixels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int height;
  /**
   * The frame rate in frames per second, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final float frameRate;
  /**
   * The clockwise rotation that should be applied to the video for it to be rendered in the correct
   * orientation, or {@link #NO_VALUE} if unknown or not applicable. Only 0, 90, 180 and 270 are
   * supported.
   */
  public final int rotationDegrees;
  /**
   * The width to height ratio of pixels in the video, or {@link #NO_VALUE} if unknown or not
   * applicable.
   */
  public final float pixelWidthHeightRatio;

  // Audio specific.

  /**
   * The number of audio channels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int channelCount;
  /**
   * The audio sampling rate in Hz, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int sampleRate;
  /**
   * The encoding for PCM audio streams. If {@link #sampleMimeType} is {@link MimeTypes#AUDIO_RAW}
   * then one of {@link C#ENCODING_PCM_8BIT}, {@link C#ENCODING_PCM_16BIT},
   * {@link C#ENCODING_PCM_24BIT} and {@link C#ENCODING_PCM_32BIT}. Set to {@link #NO_VALUE} for
   * other media types.
   */
  public final int pcmEncoding;
  /**
   * The number of samples to trim from the start of the decoded audio stream.
   */
  public final int encoderDelay;
  /**
   * The number of samples to trim from the end of the decoded audio stream.
   */
  public final int encoderPadding;

  // Text specific.

  /**
   * For samples that contain subsamples, this is an offset that should be added to subsample
   * timestamps. A value of {@link #OFFSET_SAMPLE_RELATIVE} indicates that subsample timestamps are
   * relative to the timestamps of their parent samples.
   */
  public final long subsampleOffsetUs;

  // Audio and text specific.

  /**
   * Track selection flags.
   */
  public final int selectionFlags;

  /**
   * The language, or null if unknown or not applicable.
   */
  public final String language;

  // Lazily initialized hashcode and framework media format.

  private int hashCode;
  private MediaFormat frameworkMediaFormat;

  // Video.

  public static Format createVideoContainerFormat(String id, String containerMimeType,
      String sampleMimeType, String codecs, int bitrate, int width, int height,
      float frameRate, List<byte[]> initializationData) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, NO_VALUE, width,
        height, frameRate, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, 0,
        null, OFFSET_SAMPLE_RELATIVE, initializationData, null);
  }

  public static Format createVideoSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int width, int height, float frameRate,
      List<byte[]> initializationData, DrmInitData drmInitData) {
    return createVideoSampleFormat(id, sampleMimeType, codecs, bitrate, maxInputSize, width,
        height, frameRate, initializationData, NO_VALUE, NO_VALUE, drmInitData);
  }

  public static Format createVideoSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int width, int height, float frameRate,
      List<byte[]> initializationData, int rotationDegrees, float pixelWidthHeightRatio,
      DrmInitData drmInitData) {
    return new Format(id, null, sampleMimeType, codecs, bitrate, maxInputSize, width, height,
        frameRate, rotationDegrees, pixelWidthHeightRatio, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE,
        NO_VALUE, 0, null, OFFSET_SAMPLE_RELATIVE, initializationData, drmInitData);
  }

  // Audio.

  public static Format createAudioContainerFormat(String id, String containerMimeType,
      String sampleMimeType, String codecs, int bitrate, int channelCount, int sampleRate,
      List<byte[]> initializationData, int selectionFlags, String language) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, channelCount, sampleRate, NO_VALUE, NO_VALUE,
        NO_VALUE, selectionFlags, language, OFFSET_SAMPLE_RELATIVE, initializationData, null);
  }

  public static Format createAudioSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int channelCount, int sampleRate,
      List<byte[]> initializationData, DrmInitData drmInitData, int selectionFlags,
      String language) {
    return createAudioSampleFormat(id, sampleMimeType, codecs, bitrate, maxInputSize, channelCount,
        sampleRate, NO_VALUE, initializationData, drmInitData, selectionFlags, language);
  }

  public static Format createAudioSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int channelCount, int sampleRate, int pcmEncoding,
      List<byte[]> initializationData, DrmInitData drmInitData, int selectionFlags,
      String language) {
    return createAudioSampleFormat(id, sampleMimeType, codecs, bitrate, maxInputSize, channelCount,
        sampleRate, pcmEncoding, NO_VALUE, NO_VALUE, initializationData, drmInitData,
        selectionFlags, language);
  }

  public static Format createAudioSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int channelCount, int sampleRate, int pcmEncoding,
      int encoderDelay, int encoderPadding, List<byte[]> initializationData,
      DrmInitData drmInitData, int selectionFlags, String language) {
    return new Format(id, null, sampleMimeType, codecs, bitrate, maxInputSize, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, channelCount, sampleRate, pcmEncoding, encoderDelay,
        encoderPadding, selectionFlags, language, OFFSET_SAMPLE_RELATIVE, initializationData,
        drmInitData);
  }

  // Text.

  public static Format createTextContainerFormat(String id, String containerMimeType,
      String sampleMimeType, String codecs, int bitrate, int selectionFlags, String language) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE,
        selectionFlags, language, OFFSET_SAMPLE_RELATIVE, null, null);
  }

  public static Format createTextSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int selectionFlags, String language, DrmInitData drmInitData) {
    return createTextSampleFormat(id, sampleMimeType, codecs, bitrate, selectionFlags, language,
        drmInitData, OFFSET_SAMPLE_RELATIVE);
  }

  public static Format createTextSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, int selectionFlags, String language, DrmInitData drmInitData,
      long subsampleOffsetUs) {
    return new Format(id, null, sampleMimeType, codecs, bitrate, NO_VALUE, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE,
        selectionFlags, language, subsampleOffsetUs, null, drmInitData);
  }

  // Image.

  public static Format createImageSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, List<byte[]> initializationData, String language, DrmInitData drmInitData) {
    return new Format(id, null, sampleMimeType, codecs, bitrate, NO_VALUE, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, 0, language,
        OFFSET_SAMPLE_RELATIVE, initializationData, drmInitData);
  }

  // Generic.

  public static Format createContainerFormat(String id, String containerMimeType,
      String sampleMimeType, int bitrate) {
    return new Format(id, containerMimeType, sampleMimeType, null, bitrate, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE,
        0, null, OFFSET_SAMPLE_RELATIVE, null, null);
  }

  public static Format createSampleFormat(String id, String sampleMimeType, String codecs,
      int bitrate, DrmInitData drmInitData) {
    return new Format(id, null, sampleMimeType, codecs, bitrate, NO_VALUE, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, 0, null,
        OFFSET_SAMPLE_RELATIVE, null, drmInitData);
  }

  /* package */ Format(String id, String containerMimeType, String sampleMimeType, String codecs,
      int bitrate, int maxInputSize, int width, int height, float frameRate, int rotationDegrees,
      float pixelWidthHeightRatio, int channelCount, int sampleRate, int pcmEncoding,
      int encoderDelay, int encoderPadding, int selectionFlags, String language,
      long subsampleOffsetUs, List<byte[]> initializationData, DrmInitData drmInitData) {
    this.id = id;
    this.containerMimeType = containerMimeType;
    this.sampleMimeType = sampleMimeType;
    this.codecs = codecs;
    this.bitrate = bitrate;
    this.maxInputSize = maxInputSize;
    this.width = width;
    this.height = height;
    this.frameRate = frameRate;
    this.rotationDegrees = rotationDegrees;
    this.pixelWidthHeightRatio = pixelWidthHeightRatio;
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.pcmEncoding = pcmEncoding;
    this.encoderDelay = encoderDelay;
    this.encoderPadding = encoderPadding;
    this.selectionFlags = selectionFlags;
    this.language = language;
    this.subsampleOffsetUs = subsampleOffsetUs;
    this.initializationData = initializationData == null ? Collections.<byte[]>emptyList()
        : initializationData;
    this.drmInitData = drmInitData;
  }

  /* package */ Format(Parcel in) {
    id = in.readString();
    containerMimeType = in.readString();
    sampleMimeType = in.readString();
    codecs = in.readString();
    bitrate = in.readInt();
    maxInputSize = in.readInt();
    width = in.readInt();
    height = in.readInt();
    frameRate = in.readFloat();
    rotationDegrees = in.readInt();
    pixelWidthHeightRatio = in.readFloat();
    channelCount = in.readInt();
    sampleRate = in.readInt();
    pcmEncoding = in.readInt();
    encoderDelay = in.readInt();
    encoderPadding = in.readInt();
    selectionFlags = in.readInt();
    language = in.readString();
    subsampleOffsetUs = in.readLong();
    int initializationDataSize = in.readInt();
    initializationData = new ArrayList<>(initializationDataSize);
    for (int i = 0; i < initializationDataSize; i++) {
      initializationData.add(in.createByteArray());
    }
    drmInitData = in.readParcelable(DrmInitData.class.getClassLoader());
  }

  public Format copyWithMaxInputSize(int maxInputSize) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize,
        width, height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  public Format copyWithSubsampleOffsetUs(long subsampleOffsetUs) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize,
        width, height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  public Format copyWithContainerInfo(String id, int bitrate, int width, int height,
      int selectionFlags, String language) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize,
        width, height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  public Format copyWithManifestFormatInfo(Format manifestFormat,
      boolean preferManifestDrmInitData) {
    String id = manifestFormat.id;
    String codecs = this.codecs == null ? manifestFormat.codecs : this.codecs;
    int bitrate = this.bitrate == NO_VALUE ? manifestFormat.bitrate : this.bitrate;
    float frameRate = this.frameRate == NO_VALUE ? manifestFormat.frameRate : this.frameRate;
    int selectionFlags = this.selectionFlags |  manifestFormat.selectionFlags;
    String language = this.language == null ? manifestFormat.language : this.language;
    DrmInitData drmInitData = (preferManifestDrmInitData && manifestFormat.drmInitData != null)
        || this.drmInitData == null ? manifestFormat.drmInitData : this.drmInitData;
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize, width,
        height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  public Format copyWithGaplessInfo(int encoderDelay, int encoderPadding) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize,
        width, height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  public Format copyWithDrmInitData(DrmInitData drmInitData) {
    return new Format(id, containerMimeType, sampleMimeType, codecs, bitrate, maxInputSize,
        width, height, frameRate, rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate,
        pcmEncoding, encoderDelay, encoderPadding, selectionFlags, language, subsampleOffsetUs,
        initializationData, drmInitData);
  }

  /**
   * Returns a {@link MediaFormat} representation of this format.
   */
  @SuppressLint("InlinedApi")
  @TargetApi(16)
  public final MediaFormat getFrameworkMediaFormatV16() {
    if (frameworkMediaFormat == null) {
      MediaFormat format = new MediaFormat();
      format.setString(MediaFormat.KEY_MIME, sampleMimeType);
      maybeSetStringV16(format, MediaFormat.KEY_LANGUAGE, language);
      maybeSetIntegerV16(format, MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
      maybeSetIntegerV16(format, MediaFormat.KEY_WIDTH, width);
      maybeSetIntegerV16(format, MediaFormat.KEY_HEIGHT, height);
      maybeSetFloatV16(format, MediaFormat.KEY_FRAME_RATE, frameRate);
      maybeSetIntegerV16(format, "rotation-degrees", rotationDegrees);
      maybeSetIntegerV16(format, MediaFormat.KEY_CHANNEL_COUNT, channelCount);
      maybeSetIntegerV16(format, MediaFormat.KEY_SAMPLE_RATE, sampleRate);
      maybeSetIntegerV16(format, "encoder-delay", encoderDelay);
      maybeSetIntegerV16(format, "encoder-padding", encoderPadding);
      for (int i = 0; i < initializationData.size(); i++) {
        format.setByteBuffer("csd-" + i, ByteBuffer.wrap(initializationData.get(i)));
      }
      frameworkMediaFormat = format;
    }
    return frameworkMediaFormat;
  }

  @Override
  public String toString() {
    return "Format(" + id + ", " + containerMimeType + ", " + sampleMimeType + ", " + bitrate + ", "
        + ", " + language + ", [" + width + ", " + height + ", " + frameRate + "]"
        + ", [" + channelCount + ", " + sampleRate + "])";
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 31 * result + (id == null ? 0 : id.hashCode());
      result = 31 * result + (containerMimeType == null ? 0 : containerMimeType.hashCode());
      result = 31 * result + (sampleMimeType == null ? 0 : sampleMimeType.hashCode());
      result = 31 * result + (codecs == null ? 0 : codecs.hashCode());
      result = 31 * result + bitrate;
      result = 31 * result + width;
      result = 31 * result + height;
      result = 31 * result + channelCount;
      result = 31 * result + sampleRate;
      result = 31 * result + (language == null ? 0 : language.hashCode());
      result = 31 * result + (drmInitData == null ? 0 : drmInitData.hashCode());
      hashCode = result;
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Format other = (Format) obj;
    if (bitrate != other.bitrate || maxInputSize != other.maxInputSize
        || width != other.width || height != other.height || frameRate != other.frameRate
        || rotationDegrees != other.rotationDegrees
        || pixelWidthHeightRatio != other.pixelWidthHeightRatio
        || channelCount != other.channelCount || sampleRate != other.sampleRate
        || pcmEncoding != other.pcmEncoding || encoderDelay != other.encoderDelay
        || encoderPadding != other.encoderPadding || subsampleOffsetUs != other.subsampleOffsetUs
        || selectionFlags != other.selectionFlags || !Util.areEqual(id, other.id)
        || !Util.areEqual(language, other.language)
        || !Util.areEqual(containerMimeType, other.containerMimeType)
        || !Util.areEqual(sampleMimeType, other.sampleMimeType)
        || !Util.areEqual(codecs, other.codecs)
        || !Util.areEqual(drmInitData, other.drmInitData)
        || initializationData.size() != other.initializationData.size()) {
      return false;
    }
    for (int i = 0; i < initializationData.size(); i++) {
      if (!Arrays.equals(initializationData.get(i), other.initializationData.get(i))) {
        return false;
      }
    }
    return true;
  }

  @TargetApi(16)
  private static void maybeSetStringV16(MediaFormat format, String key, String value) {
    if (value != null) {
      format.setString(key, value);
    }
  }

  @TargetApi(16)
  private static void maybeSetIntegerV16(MediaFormat format, String key, int value) {
    if (value != NO_VALUE) {
      format.setInteger(key, value);
    }
  }

  @TargetApi(16)
  private static void maybeSetFloatV16(MediaFormat format, String key, float value) {
    if (value != NO_VALUE) {
      format.setFloat(key, value);
    }
  }

  // Parcelable implementation.

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(containerMimeType);
    dest.writeString(sampleMimeType);
    dest.writeString(codecs);
    dest.writeInt(bitrate);
    dest.writeInt(maxInputSize);
    dest.writeInt(width);
    dest.writeInt(height);
    dest.writeFloat(frameRate);
    dest.writeInt(rotationDegrees);
    dest.writeFloat(pixelWidthHeightRatio);
    dest.writeInt(channelCount);
    dest.writeInt(sampleRate);
    dest.writeInt(pcmEncoding);
    dest.writeInt(encoderDelay);
    dest.writeInt(encoderPadding);
    dest.writeInt(selectionFlags);
    dest.writeString(language);
    dest.writeLong(subsampleOffsetUs);
    int initializationDataSize = initializationData.size();
    dest.writeInt(initializationDataSize);
    for (int i = 0; i < initializationDataSize; i++) {
      dest.writeByteArray(initializationData.get(i));
    }
    dest.writeParcelable(drmInitData, 0);
  }

  public static final Creator<Format> CREATOR = new Creator<Format>() {

    @Override
    public Format createFromParcel(Parcel in) {
      return new Format(in);
    }

    @Override
    public Format[] newArray(int size) {
      return new Format[size];
    }

  };

}
