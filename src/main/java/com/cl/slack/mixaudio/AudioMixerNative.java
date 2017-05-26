package com.cl.slack.mixaudio;

//javah -classpath bin/classes -d jni com.kaolafm.record.AudioMixerNative

/**
 * 后期处理视频编辑背景音乐思路：
 * 1.背景音乐解码，
 * 2.视频解码，获取音频部分
 * 如何同步，
 */
public class AudioMixerNative {
    // init pcm mix encoder
	public native int PcmMixEncoderInit();

    public native int PcmMixEncoderInitWithParams(int iSampleRate, int iChannelNumber);

    // destroy pcm mix encoder
	public native void PcmMixEncoderDeInit();

    /**
     * 先输入 mic ,再调用 MusicPcmMixEncode 输入 背景音乐，返回混合编码数据
     * 此处有些奇怪，输入 背景音乐，但是没有提供单独输入 mic 的方法，
     * MicPcmEncode 输入 mic 然后读取队列里之前所有的数据进行编码返回
     */
	public native byte[] MusicPcmMixEncode(int iSampleRate, int iChannelNumber, 
			byte[] pData, int iLen);
    // 先输入 背景音乐 ,再调用 MicPcmMixEncode 输入 mic 数据，返回混合编码数据
	public native byte[] MicPcmMixEncode(int iSampleRate, int iChannelNumber, 
			byte[] pData, int iLen);
    // 输入 背景音乐 返回编码数据
    public native byte[] MusicPcmEncode(int iSampleRate, int iChannelNumber, 
    		byte[] pData, int iLen);
    // 输入 mic 数据 返回编码数据
    public native byte[] MicPcmEncode(int iSampleRate, int iChannelNumber, 
    		byte[] pData, int iLen);
    // 输入 mic 背景音乐 后 主动调用一次 混合编码，返回 mic 和 背景音乐 混合编码数据
    public native byte[] PcmMixFlush();
    // 混合时 背景音乐 权重
    public native void MusicGain(float fMusicGain);
    // 混合时 mic(麦克风数据－用户说话等) 权重
    public native void MicGain(float fMicGain);
    // audio file cut 裁剪
    public native int audioFileCut(String InputFilePathname, String OutputFilePathname, 
    		float fStartTime, float fEndTime, float fBitRate);

    /**
     * 新增 输入 背景音乐 添加到处理队列
     */
    public native void addMusicPcmQueue(int iSampleRate, int iChannelNumber, byte[] pData);

    /**
     * 新增 输入 mic 数据 返回编码数据
     */
    public native void addMicPcmQueue(int iSampleRate, int iChannelNumber, byte[] pData);

    /**
     * 混合 两个 音频 片段
     * 视频原音 ， 背景音乐
     * 直接一帧一帧 的混合
     */
    public native byte[]  mixTwoPcmFlush(int iSampleRate1, int iChannelNumber1, byte[] pData1, int iSampleRate2, int iChannelNumber2,byte[] pData2);

    public native byte[]  mixTwoPcmFlushWithDefault(int defautlSamplerate, int defaultChannelCount, int iSampleRate1, int iChannelNumber1, byte[] pData1, int iSampleRate2, int iChannelNumber2,byte[] pData2);

    public native void clearQueue();

    static {
        System.loadLibrary("recordsdk");  
    }
}