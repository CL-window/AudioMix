package com.cl.slack.mixaudio;

//javah -classpath bin/classes -d jni com.kaolafm.record.AudioMixerNative

public class AudioMixerNative {
    // init pcm mix encoder
	public native int PcmMixEncoderInit();
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
    public native void addMusicPcmQueue(int iSampleRate, int iChannelNumber,byte[] pData);

    /**
     * 新增 输入 mic 数据 返回编码数据
     */
    public native void addMicPcmQueue(byte[] pData);

    public native void clearQueue();

    static {
        System.loadLibrary("recordsdk");  
    }
}