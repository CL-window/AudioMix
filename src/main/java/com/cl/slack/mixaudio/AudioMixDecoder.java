//package com.cl.slack.mixaudio;
//
//import android.media.MediaCodec;
//import android.media.MediaExtractor;
//import android.media.MediaFormat;
//import android.text.TextUtils;
//
//import com.benqu.core.jni.WTJNIWrapper;
//import com.benqu.core.media.MediaHelper;
//import com.benqu.core.util.D;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.ArrayDeque;
//import java.util.Queue;
//
///**
// * Created by slack
// * on 17/5/19 下午12:25
// * 负责 视频里音频的处理
// * 1.获取数据源 {@link MediaDecoder#mExtractor}
// * MediaCodec input(raw)-->output(pcm)
// * 2.音频混合(pcm)
// * 3.MediaCodec 编码 input(pcm)-->output(rwa)
// * 4.交付 {@link MediaEncoder#mMuxer}
// */
//
//class AudioMixDecoder {
//
//    /** default 1 channelCount */
//    private static final int DEFAULT_CHANNEL_COUNT = 1;
//    private final static int TIMEOUT_USEC = 10000;
//    private MediaCodec mAudioDecoder;
//    private MediaCodec mAudioEncoder;
//
//    private MediaCodec.BufferInfo mAudioDecodeInfo;
//    private MediaCodec.BufferInfo mAudioEncodeInfo;
//
//    private AudioMixQueue mWTAudioMixQueue;
//    private int mVideoAudioSampleRate = MediaHelper.Audio.SAMPLE_RATE;
//    private int mVideoAudioChannelCount = DEFAULT_CHANNEL_COUNT;
//
//    private boolean mStopDecodeFlag = false;
//
//    AudioMixDecoder(MediaInfo info) throws IOException {
//        mAudioDecoder = MediaCodec.createDecoderByType(info.mAudioMime);
//        mAudioDecoder.configure(info.mAudioFormat, null, null, 0);
//
//        initAudioFormat(info);
//
//        mAudioEncoder = MediaCodec.createEncoderByType(info.mAudioMime);
//        initAudioEncodeFormat();
//
//        mAudioDecodeInfo = new MediaCodec.BufferInfo();
//        mAudioEncodeInfo = new MediaCodec.BufferInfo();
//    }
//
//    private AudioMixDecodeListener mAudioMixDecodeListener;
//
//    public void setListener(AudioMixDecodeListener l) {
//        mAudioMixDecodeListener = l;
//    }
//
//    void setMicMixWeight(float weight) {
//        JNIWrapper.audioMixMicGain(weight);
//    }
//
//    void setMusicMixWeight(float weight) {
//        JNIWrapper.audioMixMusicGain(weight);
//    }
//
//    private String mMixMusicPath;
//    void updateBackgroundMusic(String filePath) {
//        mMixMusicPath = filePath;
//        if (needMixMusic()) {
//            mWTAudioMixQueue = new AudioMixQueue(mMixMusicPath);
//            mWTAudioMixQueue.startAudioExtractor();
//            JNIWrapper.audioMixEncoderInit(mVideoAudioSampleRate, mVideoAudioChannelCount);
//        }
//    }
//
//    private void initAudioEncodeFormat() {
//        MediaFormat audio = MediaFormat.createAudioFormat(
//                MediaHelper.Audio.MIME_TYPE, MediaHelper.Audio.SAMPLE_RATE, DEFAULT_CHANNEL_COUNT);
//        audio.setInteger(MediaFormat.KEY_AAC_PROFILE, 2);
//        audio.setInteger(MediaFormat.KEY_BIT_RATE, MediaHelper.Audio.BIT_RATE);
//        mAudioEncoder.configure(audio, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//    }
//
//    private boolean needMixMusic() {
//        return !TextUtils.isEmpty(mMixMusicPath);
//    }
//
//    private void initAudioFormat(MediaInfo info) {
//        MediaFormat format = info.mAudioFormat;
//        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
//            mVideoAudioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        }
//        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
//            mVideoAudioChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//        }
//    }
//
//    public void start() {
//        if (needMixMusic()) {
//            mStopDecodeFlag = false;
//            mAudioDecoder.start();
//            mAudioEncoder.start();
//        }
//    }
//
//    private void stop() {
//        mAudioDecoder.stop();
//        mAudioEncoder.stop();
//        mStopDecodeFlag = true;
//        D.i("slack", "Audio encode stop... ");
//    }
//
//    /**
//     * 最后调用，分离器分离结束时调用，等待未完成数据，写入全部数据
//     */
//    public void decodeRemainAudio() {
//
//        try {
//            mixAudioData(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            onDecodeError();
//        }
//
//        try {
//            encodeAudioData(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            onDecodeError();
//        }
//
//        stop();
//    }
//
//    public void release() {
//        mOriginTimeStamp.clear();
//        if (mWTAudioMixQueue != null) {
//            mWTAudioMixQueue.release();
//        }
//        JNIWrapper.audioMixEncoderDeInit();
//    }
//
//    /** 时间戳 以 MediaExtractor 原始时间为准, 记录每一帧 音频 的时间戳 */
//    private final Queue<Long> mOriginTimeStamp = new ArrayDeque<>();
//    void mixOneAudioFrameIfNeed(MediaExtractor extractor, MediaFrame mediaFrame) {
//        if (needMixMusic() && mWTAudioMixQueue != null) {
//            try{
//                readDataFromExtractor(extractor, mediaFrame);
//            }catch (Exception e){
//                e.printStackTrace();
//                onlyOriginalData(extractor, mediaFrame);
//                onDecodeError();
//            }
//
//            try{
//                mixAudioData(false);
//            }catch (Exception e){
//                e.printStackTrace();
//                onlyOriginalData(extractor, mediaFrame);
//                onDecodeError();
//            }
//
//            try{
//                encodeAudioData(false);
//            }catch (Exception e){
//                e.printStackTrace();
//                onlyOriginalData(extractor, mediaFrame);
//                onDecodeError();
//            }
//
//        } else {
//            onlyOriginalData(extractor, mediaFrame);
//        }
//    }
//
//    private void encodeAudioData(boolean endOfStream) {
//        while (!mStopDecodeFlag){
//            int encoderIndex = mAudioEncoder.dequeueOutputBuffer(mAudioEncodeInfo, TIMEOUT_USEC);
//            if (encoderIndex >= 0) {
//                ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
//                ByteBuffer encodeData = outputBuffers[encoderIndex];
//                if (mAudioEncodeInfo.size != 0) {
//                    if ((mAudioEncodeInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                        mAudioEncoder.releaseOutputBuffer(encoderIndex, false);
//                        return;
//                    }
//
//                    encodeData.position(mAudioEncodeInfo.offset);
//                    encodeData.limit(mAudioEncodeInfo.offset + mAudioEncodeInfo.size);
//                    Long time = mOriginTimeStamp.poll();
//                    if(time != null) {
//                        mAudioEncodeInfo.presentationTimeUs = time;
//                    }
//                    if (mAudioMixDecodeListener != null) {
//                        D.i("slack","onAudioFrameEncode time size :" + mOriginTimeStamp.size() + " current time : " + mAudioEncodeInfo.presentationTimeUs);
//                        mAudioMixDecodeListener.onAudioFrameEncode(encodeData,mAudioEncodeInfo);
//                    }
//                }
//
//                // 如果是结束帧, 就结束
//                if ((mAudioEncodeInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    mAudioEncoder.releaseOutputBuffer(encoderIndex, false);
//                    D.i("slack","mAudioEncoder end of stream...");
//                    break;
//                }
//
//                mAudioEncoder.releaseOutputBuffer(encoderIndex, false);
//            }else {
//                if (encoderIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    if(endOfStream){
//                        D.i("slack","mAudioDecoder wait end of stream...");
//                    }else {
//                        break;
//                    }
//                } else if (encoderIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    D.i("slack","out put change mAudioEncoder : " + mAudioEncoder.getOutputFormat().toString());
//                    if (mAudioMixDecodeListener != null) {
//                        mAudioMixDecodeListener.
//                                onAudioMixOutputFormatChanged(mAudioEncoder.getOutputFormat());
//                    }
//                }
//            }
//        }
//    }
//
//    private void mixAudioData(boolean endOfStream) {
//
//        if(endOfStream) {
//            sendSingleEndOfEncode(mAudioDecoder);
//        }
//
//        // output(pcm)
//        while (!mStopDecodeFlag) {
//            int outputIndex = mAudioDecoder.dequeueOutputBuffer(mAudioDecodeInfo, TIMEOUT_USEC);
//            if (outputIndex >= 0) {
//                // Simply ignore codec config buffers.
//                if ((mAudioDecodeInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    mAudioDecoder.releaseOutputBuffer(outputIndex, false);
//                    return;
//                }
//
//                if (mAudioDecodeInfo.size != 0) {
//                    ByteBuffer[] outputBuffers = mAudioDecoder.getOutputBuffers();
//                    ByteBuffer outBuf = outputBuffers[outputIndex];
//
//                    outBuf.position(mAudioDecodeInfo.offset);
//                    outBuf.limit(mAudioDecodeInfo.offset + mAudioDecodeInfo.size);
//                    byte[] data = new byte[mAudioDecodeInfo.size];
//                    outBuf.get(data);
//
//                    // mix (pcm)
//                    byte[] result = mixAudioSrcAndBackMusicData(data);
//                    // input(pcm)
//                    reEncodeAudioInInput(result);
//
//                }
//
//                // 如果是结束帧, 就结束
//                if ((mAudioDecodeInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    mAudioDecoder.releaseOutputBuffer(outputIndex, false);
//                    D.i("slack","mAudioDecoder end of stream...");
//                    sendSingleEndOfEncode(mAudioEncoder);
//                    break;
//                }
//
//                mAudioDecoder.releaseOutputBuffer(outputIndex, false);
//            }
//            else {
//                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    if(endOfStream){
//                        D.i("slack","mAudioDecoder wait end of stream...");
//                    }else {
//                        break;
//                    }
//                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    D.i("slack","out put change mAudioDecoder : " + mAudioDecoder.getOutputFormat().toString());
//                }
//            }
//        }
//    }
//
//    private boolean sendSingleEndOfEncode(MediaCodec codec) {
//        int inputIndex = codec.dequeueInputBuffer(-1);
//        if (inputIndex >= 0) {
//            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//        }else {
//            stop();
//            return false;
//        }
//        return true;
//    }
//
//    private void readDataFromExtractor(MediaExtractor extractor, MediaFrame mediaFrame) {
//        int inputBufIndex = mAudioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
//        if (inputBufIndex >= 0) {
//            // input(raw)
//            ByteBuffer[] inputBuffers = mAudioDecoder.getInputBuffers();
//            ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
//            inputBuffer.clear();
//            long time = extractor.getSampleTime();
//            mediaFrame.set(false,
//                    extractor.readSampleData(inputBuffer, 0),
//                    time,
//                    extractor.getSampleFlags());
//
//            mOriginTimeStamp.add(time);
//            D.i("slack", "mAudioDecoder audio time last time : " + time);
//
//            mAudioDecoder.queueInputBuffer(
//                    inputBufIndex, 0, mediaFrame.size(), mediaFrame.time(), 0);
//            extractor.advance();
//        }
//    }
//
//    private void onDecodeError(){
//        mMixMusicPath = "";
//        if (mAudioMixDecodeListener != null) {
//            mAudioMixDecodeListener.onAudioMixError();
//        }
//    }
//
//    private void reEncodeAudioInInput(byte[] data){
//        try{
//            ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
//            int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
//            if (inputBufferIndex >= 0 && data != null) {
//                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
//                inputBuffer.clear();
//                inputBuffer.limit(data.length);
//                inputBuffer.put(data);
//                mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, 0, 0);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    private void onlyOriginalData(MediaExtractor extractor, MediaFrame mediaFrame) {
//        mediaFrame.set(true,
//                extractor.readSampleData(mediaFrame.buffer, 0),
//                extractor.getSampleTime(),
//                extractor.getSampleFlags());
//        extractor.advance();
//    }
//
//    /**
//     * mix two audio/raw
//     * @param data
//     * @return
//     */
//    private byte[] mixAudioSrcAndBackMusicData(byte[] data) {
//
//        byte[] music = mWTAudioMixQueue.getAudioDataCycle();
//        if (music == null) {
//            D.e("slack", "back music is null...");
//            return data;
//        }
//
//        return JNIWrapper.mixTwoPcmFlush(mVideoAudioSampleRate, mVideoAudioChannelCount, data,
//                mWTAudioMixQueue.getSampleRate(), mWTAudioMixQueue.getChannelCount(), music);
//
//    }
//
//    interface AudioMixDecodeListener {
//
//        /**
//         * start Muxer, 应该只能被调用一次
//         * 设置这个监听，如果需要混合音乐，需要等待此回调，混合器才可以start
//         * call first
//         */
//        void onAudioMixOutputFormatChanged(MediaFormat format);
//
//        /**
//         * call when one audio frame encode ok
//         * @param buffer buffer
//         * @param info buffer info
//         */
//        void onAudioFrameEncode(ByteBuffer buffer, MediaCodec.BufferInfo info);
//
//        void onAudioMixError();
//    }
//}
