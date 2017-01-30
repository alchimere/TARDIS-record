package com.firetonton.tardisrecord;

import org.junit.Test;

import com.firetonton.tardisrecord.helpers.LossyCircularBuffer;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
/**
 * Created by bertho_y on 1/18/17.
 */

public class LossyCircularBufferUnitTest {
    @Test
    public void getMaxLength_isCorrect() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(4);
        assertEquals(4, buff.getMaxLength());
    }

    @Test
    public void getLength_onEmptyBuffer_isCorrect() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(4);
        assertEquals(0, buff.getLength());
    }

    @Test
    public void addByte_addsOneToLength() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(4);

        buff.addByte((byte) '*');

        assertEquals(1, buff.getLength());
    }

    @Test
    public void add_addsNBytesToLength() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(6);

        buff.add("4242".getBytes(), 4);

        assertEquals(4, buff.getLength());
    }

    @Test
    public void add_tooManyBytes_RemoveTheBegining() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(6);

        buff.add("42427878".getBytes());

        assertEquals(6, buff.getLength());
        assertEquals("4278", new String(buff.takeBytes(4)));
    }

    @Test
    public void takeBytes_RemovesThem() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(6);

        buff.add("42427878".getBytes());

        assertEquals(6, buff.getLength());
        assertEquals(4, new String(buff.takeBytes(4)).length());
        assertEquals(2, buff.getLength());
    }

    @Test
    public void takeBytes_ifWantedSizeIsHigherThanFilledSize_returnsMinSize() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(10);

        buff.add("4242".getBytes());

        assertEquals(4, buff.getLength());
        assertEquals(4, new String(buff.takeBytes(10)).length());
        assertEquals(0, buff.getLength());
    }



    @Test
    public void copyLastBytes_ifWantedSizeIsHigherThanFilledSize_returnsMinSize() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(10);

        buff.add("4242".getBytes());

        assertEquals(4, buff.getLength());
        assertEquals(4, new String(buff.copyLastBytes(10)).length());
        assertEquals(4, buff.getLength());
    }

    @Test
    public void copyLastBytes_DoesNotRemoveThem() throws Exception {
        LossyCircularBuffer buff = new LossyCircularBuffer(10);

        buff.add("42427878".getBytes());

        assertEquals(8, buff.getLength());
        assertEquals("7878", new String(buff.copyLastBytes(4)));
        assertEquals(8, buff.getLength());
    }
}
