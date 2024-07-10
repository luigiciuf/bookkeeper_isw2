package org.apache.bookkeeper.bookie;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FileInfoTest {


    public ByteBuffer bb;

    public long start;
    public Object result;
    public boolean bestEffort;
    private FileInfo fileInfoUnderTest;
    private FileChannel fileChannelMock;


    private FileChannel fc;
    public Object expectedException;

    public FileInfoTest(ByteBuffer bb, long start, boolean bestEffort, Object expectedException) {
        this.bb = bb;
        this.start = start;
        this.bestEffort = bestEffort;
        this.expectedException = expectedException;

    }

    @After
    public void tearDown() {
        // Eventuali operazioni di pulizia
        fileInfoUnderTest = null;
    }

    @Before
    public void setUp() throws Exception {

        File tempFile = File.createTempFile("testfile", ".txt");
        tempFile.deleteOnExit();


        byte[] masterKey = new byte[]{0x01, 0x02, 0x03};
        fileInfoUnderTest = new FileInfo(tempFile, masterKey, 1);


        FileChannel fileChannelMock = mock(FileChannel.class);

        // Configure the mock to return specific values for the read method
        when(fileChannelMock.read(any(ByteBuffer.class), anyLong()))
                .thenAnswer(invocation -> {
                    ByteBuffer buffer = invocation.getArgument(0);
                    int remaining = buffer.remaining();
                    if (remaining == 0) {
                        return 0; // Simulate rc == 0
                    } else if (remaining > 0 && remaining <= 5) {
                        buffer.put(new byte[remaining]); // Fill the buffer
                        return remaining; // Simulate rc > 0
                    } else {
                        return -1; // Simulate rc < 0
                    }
                });

        java.lang.reflect.Field fcField = FileInfo.class.getDeclaredField("fc");
        fcField.setAccessible(true);
        fcField.set(fileInfoUnderTest, fileChannelMock);

    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, -1, true, NullPointerException.class},
                {null, 0, false, NullPointerException.class},
                {null, 1, true, NullPointerException.class},


                {ByteBuffer.allocate(0), -1, true, 0},
                {ByteBuffer.allocate(0), 0, false, ShortReadException.class},
                {ByteBuffer.allocate(0), 1, true, 0},

                //{ByteBuffer.allocate(5), -1, true,IllegalArgumentException.class},
                {ByteBuffer.allocate(5), 0, false, ShortReadException.class},
                {ByteBuffer.allocate(5), 1, true, 5},

                // {ByteBuffer.wrap("data+1".getBytes()), -1, true, IllegalArgumentException.class},
                {ByteBuffer.wrap("data+1".getBytes()), 0, false, ShortReadException.class},
                // {ByteBuffer.wrap("data+1".getBytes()), 1, true,6},


                {ByteBuffer.allocate(5), 0, true, 5}, // Simulate read less than buffer size, bestEffort = true
                {ByteBuffer.allocate(10), 0, true, 0}, // Simulate partial read and bestEffort = true




        });
    }

    @Test
    public void testReadAbsolute() throws IOException {
        try {

            int result = fileInfoUnderTest.readAbsolute(bb, start, bestEffort);

            //assertEquals(5, result); // Esempio di asserzione
            if (expectedException == NullPointerException.class) {

            } else if (expectedException == ShortReadException.class) {

            } else {
                assertEquals(expectedException, result);
            }


        } catch (Exception e) {
            if (expectedException != null && expectedException != e.getClass()) {
                fail("Unexpected exception thrown: " + e.getClass());
            }
        }


    }


    @Test
    public void testReadAbsoluteWhenFcIsNull() throws IOException {
        try {
            java.lang.reflect.Field fcField = FileInfo.class.getDeclaredField("fc");
            fcField.setAccessible(true);
            fcField.set(fileInfoUnderTest, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set 'fc' field to null: " + e.getMessage());
        }

        ByteBuffer bb = ByteBuffer.allocate(0);
        int result = fileInfoUnderTest.readAbsolute(bb, 0, false);
        assertEquals(0, result);
    }


    @Test
    public void testcheckOpen() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FileInfo fileInfoMock = spy(fileInfoUnderTest);
        fileInfoMock.readAbsolute(ByteBuffer.allocate(5), 0, false);
        verify(fileInfoMock).checkOpen(false);
    }

    @Test
    public void testReadAbsolutePit() throws IOException {
        // Mock del canale file per simulare varie condizioni di rc
        FileChannel fileChannelMock = mock(FileChannel.class);
        when(fileChannelMock.read(any(ByteBuffer.class), anyLong()))
                .thenReturn(5)    // Simula rc > 0
                .thenReturn(0)    // Simula rc == 0
                .thenReturn(-1);  // Simula rc < 0

        try {
            Field fcField = FileInfo.class.getDeclaredField("fc");
            fcField.setAccessible(true);
            fcField.set(fileInfoUnderTest, fileChannelMock);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set 'fc' field: " + e.getMessage());
        }

        ByteBuffer bb = ByteBuffer.allocate(10);
        int result1 = fileInfoUnderTest.readAbsolute(bb, 0, true);
        int result2 = fileInfoUnderTest.readAbsolute(bb, 0, true);
        int result3 = fileInfoUnderTest.readAbsolute(bb, 0, true);

        verify(fileChannelMock, times(4)).read(any(ByteBuffer.class), anyLong());

        assertEquals(5, result1);
        assertEquals(0, result2);
        assertEquals(0, result3);
        assertThrows(ShortReadException.class, () -> fileInfoUnderTest.readAbsolute(ByteBuffer.allocate(10), 0, false));

        assertThrows(ShortReadException.class, () -> fileInfoUnderTest.readAbsolute(ByteBuffer.allocate(10), 0, false));
        assertEquals(0, fileInfoUnderTest.readAbsolute(ByteBuffer.allocate(10), 0, true));

    }


    }










