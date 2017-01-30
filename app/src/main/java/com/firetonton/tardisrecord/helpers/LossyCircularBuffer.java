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
}
