from : https://github.com/StemonZhang/AudioMix
Mix audio with background music
#编译c++ 代码
cd mixaudio/src/main/jni/
ndk-build


音频混合：
channelCount 关键
如果视频的音乐是单通道，背景音乐是双通道，我的处理是传人默认channelCount参数，默认处理为单通道
通道处理不对，混合出来的音频不对！！！
时间戳 以 MediaExtractor 解码出时间为准，做一个队列存储解码出来的时间

混合Audio流程：(MediaCodec 编码，视频音频部分时间戳为标准时间戳)

1.MediaExtractor 分离视频中音频   [存储视频音频部分原始时间戳]
2.MediaCodecDecoder queueInputBuffer (解码 1 分离出来的数据)
3.MediaCodecDecoder dequeueOutputBuffer 获取 2 解码出来的数据
4.mix
5.MediaCodecEncoder queueInputBuffer (编码 4 混合好的数据)
6.MediaCodecEncoder dequeueOutputBuffer 获取 5 编码出来的数据   [使用视频原始时间戳]
7.通知 MediaMuxer 写入一帧音频