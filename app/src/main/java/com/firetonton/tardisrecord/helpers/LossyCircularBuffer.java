package com.firetonton.tardisrecord.helpers;

import android.util.Log;

/**
 * Created by bertho_y on 1/18/17.
 */

public class LossyCircularBuffer {
    private byte mArray[];
    private int mArraySize = 0;
    private int mPosStart = 0;
    private int mFilledSize = 0;

    public LossyCircularBuffer(int size) {
        mArraySize = size;
        mArray = new byte[mArraySize];
    }

    public void add(byte buff[]) {
        add(buff, buff.length);
    }
    public synchronized void add(byte buff[], int size) {
        for (int i = 0; i < size; i++) {
            addByte(buff[i]);
        }
    }

    public int getMaxLength() {
        return mArraySize;
    }

    public int getLength() {
        return mFilledSize;
    }

    public synchronized void addByte(byte b) {
        int insertPos = (mPosStart + mFilledSize) % mArraySize;
        mArray[insertPos] = b;
        if (mFilledSize == mArraySize)
            mPosStart = (mPosStart + 1) % mArraySize;
        else
            mFilledSize++;
    }

    public byte[] takeAllBytes() {
        return takeBytes(getLength());
    }

    public synchronized byte[] takeBytes(int size) {
        if (size > mFilledSize)
            size = mFilledSize;
        byte result[] = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = mArray[(mPosStart + i) % mArraySize];
        }
        mPosStart = (mPosStart + size) % mArraySize;
        mFilledSize -= size;
        return result;
    }

    public void logState() {
        Log.d("LossyCircularBuffer",
                "mArraySize: "  + mArraySize + ", " +
                "mPosStart: "   + mPosStart  + ", " +
                "mFilledSize: " + mFilledSize);
    }

    /* Method 1 - Must allocate big buffer */
    public synchronized byte[] copyLastBytes(int nbBytes) {
        if (nbBytes > mFilledSize)
            nbBytes = mFilledSize;
        byte result[] = new byte[nbBytes];
        int posStart = mPosStart + mFilledSize - nbBytes;
        for (int i = 0; i < nbBytes; i++) {
            result[i] = mArray[(posStart + i) % mArraySize];
        }
        return result;
    }

    /* Method 2 - Don't release lock until method end */
    public interface BufferPartHandler {
        void onCopyBufferPart(byte[] bufferPart, int bufferSize);
    }

    public synchronized void forEachLastBytes(int nbBytes, int buffSize, BufferPartHandler handler) {
        if (nbBytes > mFilledSize)
            nbBytes = mFilledSize;
        byte buff[] = new byte[buffSize];
        int nbFullBuffers = nbBytes / buffSize;
        int posStart = mPosStart + mFilledSize - nbBytes;

        // Copy each full buffer
        for (int n = 0; n < nbFullBuffers; n++) {
            for (int i = 0; i < buffSize; i++)
                buff[i] = mArray[(posStart + n * buffSize + i) % mArraySize];
            handler.onCopyBufferPart(buff, buffSize);
        }

        if (nbBytes % buffSize > 0) {
            for (int i = 0; i < nbBytes % buffSize; i++)
                buff[i] = mArray[(posStart + nbFullBuffers * buffSize + i) % mArraySize];
            handler.onCopyBufferPart(buff, nbBytes % buffSize);
        }
    }
}
