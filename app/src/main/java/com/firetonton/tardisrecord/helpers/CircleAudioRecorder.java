package com.firetonton.tardisrecord.helpers;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.firetonton.tardisrecord.helpers.LossyCircularBuffer;

/**
 * Created by bertho_y on 1/16/17.
 */

public class CircleAudioRecorder {
    private static final int SAMPLE_RATE = 22100;
    private static final int BITS_PER_SAMPLE = 16;
    private AudioRecord ar = null;
    private int mBuffsize = 0;
    private LossyCircularBuffer mCircBuf;

    /* Parameters */
    private int mAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    //        int myBitsPerSample= 16;
//        int myFormat = 1;
//        long myChannels = 1;
//        long mySampleRate = 22100;
//        long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
    public int mCircBufferSize = (SAMPLE_RATE * 1 * 16 / 8) * (30 * 60); // byterate * 15min
    //public int mCircBufferSize = 42 * 1024 * 1024; // 42 Mo (~33min)

    private boolean isRecording = false;
    private Thread recordingThread = null;


    public interface ProgressListener {
        void onProgress(long bufferedRecordDuration, long totalRecordDuration);
    }

    private ProgressListener mProgressListener;

    /* Setters */
    public void setAudioSource(int mAudioSource) {
        this.mAudioSource = mAudioSource;
    }
    public void setCircBufferSize(int mCircBufferSize) {
        this.mCircBufferSize = mCircBufferSize;
    }
    public void setProgressListener(ProgressListener listener) { this.mProgressListener = listener; };

    /* Start */
    public void startRecord()
    {
        if (isRecording)
            return ;

        mBuffsize = calcBytePerSecond();
        ar = new AudioRecord(
                mAudioSource,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBuffsize
        );

        // if (NoiseSuppressor.isAvailable())
        //     NoiseSuppressor.create(ar.getAudioSessionId());

        ar.startRecording();

        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                //writeAudioDataToFile();
                doRecord();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void doRecord() {
        byte bData[] = new byte[mBuffsize];

        /* Calc max memory available */
        Runtime rt = Runtime.getRuntime();
        int maxMemory = (int)(rt.maxMemory() * 0.8); // Take maximum 80% of max memory
        Log.v("doRecord", "maxMemory: " + maxMemory);
        /* ************************* */

        int circBufSize = Math.min(mCircBufferSize, maxMemory);
        Log.v("doRecord", "circBufSize: " + circBufSize);
        mCircBuf = new LossyCircularBuffer(circBufSize);

        long startTime = System.currentTimeMillis();
        int bps = calcBytePerSecond();
        int globalReadSize = 0;
        while (isRecording) {
            int readSize = ar.read(bData, 0, mBuffsize);
            if (readSize > 0)
                mCircBuf.add(bData, readSize);
            globalReadSize += readSize;
            if (globalReadSize >= bps) {
                notifyProgress(mCircBuf.getLength() / bps, startTime);
                globalReadSize = 0;
            }
        }

        mCircBuf.logState();
    }

    private void notifyProgress(int bufferedRecordDuration, long startTime) {
        if (mProgressListener != null) {
            mProgressListener.onProgress(
                    bufferedRecordDuration,
                    (System.currentTimeMillis() - startTime) / 1000
            );
        }
    }

    public void writeLastNSecondsToFile(int duration, String filePath) throws IOException {
        //writeBufferToAudioFile(mCircBuf.copyLastBytes(getByteSizeForDuration(duration)), filePath);

        if (filePath == null)
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + "TardisRecord/voice8K16bitmono-"+System.currentTimeMillis()+".wav";

        int dataLength = getByteSizeForDuration(duration);
        if (mCircBuf.getLength() < dataLength)
            dataLength = mCircBuf.getLength();

        byte bHeader[] = getWAVHeader(dataLength, SAMPLE_RATE, BITS_PER_SAMPLE);
        final FileOutputStream os = new FileOutputStream(filePath);

        os.write(bHeader, 0, bHeader.length);
        mCircBuf.forEachLastBytes(dataLength, 100 * 1024, new LossyCircularBuffer.BufferPartHandler() {
            @Override
            public void onCopyBufferPart(byte[] bufferPart, int bufferSize) {
                try {
                    Log.d("WRITE", "Writing "+bufferSize+" bytes");
                    os.write(bufferPart, 0, bufferSize);
                } catch (IOException e) {
                    e.printStackTrace(); // ?
                }
            }
        });
    }

    private int getByteSizeForDuration(int duration) {
        return duration * calcBytePerSecond();
    }

    public void stopRecord()
    {
        ar.stop();
        isRecording = false;
        try {
            recordingThread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeBufferToAudioFile(byte[] bData, String filePath) throws IOException {
        if (filePath == null)
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + "TardisRecord/voice8K16bitmono-"+System.currentTimeMillis()+".wav";

        byte bHeader[] = getWAVHeader(bData.length, SAMPLE_RATE, BITS_PER_SAMPLE);
        FileOutputStream os = new FileOutputStream(filePath);
        os.write(bHeader, 0, bHeader.length);
        os.write(bData, 0, bData.length);
    }

    private int calcBytePerSecond() {
        int sampleRate = SAMPLE_RATE;
        int nbChannels = 1;
        int bitsPerSample = BITS_PER_SAMPLE;
        return sampleRate * nbChannels * bitsPerSample / 8;
    }

    public byte[] getWAVHeader(int totalDataLen, int sampleRate, int bitsPerSample // ex: 16
                                ) {
        int nbChannels = 1;

//        long mySubChunk1Size = 16;
//        int myBitsPerSample= 16;
//        int myFormat = 1;
//        long myChannels = 1;
//        long mySampleRate = 22100;
//        long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
//        int myBlockAlign = (int) (myChannels * myBitsPerSample/8);
//
//        byte[] clipData = getBytesFromFile(fileToConvert);
//
//        long myDataSize = clipData.length;
//        long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
//        long myChunkSize = 36 + myChunk2Size;

        ByteBuffer byteBuffer = ByteBuffer.allocate(44);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.put("RIFF".getBytes(Charset.forName("UTF-8")));
        byteBuffer.putInt(totalDataLen+36);
        byteBuffer.put("WAVE".getBytes(Charset.forName("UTF-8")));
        byteBuffer.put("fmt ".getBytes(Charset.forName("UTF-8")));
        byteBuffer.putInt(16); // "WAVEfmt " size
        byteBuffer.putShort((short) 1); // Format: 1 = PCM
        byteBuffer.putShort((short) nbChannels); // nb channels (1 = mono, 2 = stereo, etc...)
        byteBuffer.putInt(sampleRate); // sample rate (ex: 22100)
        byteBuffer.putInt(sampleRate * nbChannels * bitsPerSample / 8); // bytes per second
        byteBuffer.putShort((short)(nbChannels * bitsPerSample / 8)); // # of bytes in one sample
        byteBuffer.putShort((short) bitsPerSample); // 16 or 32
        byteBuffer.put("data".getBytes(Charset.forName("UTF-8")));
        byteBuffer.putInt(totalDataLen);

//        outFile.writeBytes("RIFF");                                 // 00 - RIFF
//        outFile.write(intToByteArray((int)myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
//        outFile.writeBytes("WAVE");                                 // 08 - WAVE
//        outFile.writeBytes("fmt ");                                 // 12 - fmt
//        outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);  // 16 - size of this chunk
//        outFile.write(shortToByteArray((short)myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
//        outFile.write(shortToByteArray((short)myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
//        outFile.write(intToByteArray((int)mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
//        outFile.write(intToByteArray((int)myByteRate), 0, 4);       // 28 - bytes per second
//        outFile.write(shortToByteArray((short)myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
//        outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
//        outFile.writeBytes("data");                                 // 36 - data
//        outFile.write(intToByteArray((int)myDataSize), 0, 4);       // 40 - how big is this data chunk
//        outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers
//
//        outFile.flush();
//        outFile.close();
        return byteBuffer.array();
    }

}
