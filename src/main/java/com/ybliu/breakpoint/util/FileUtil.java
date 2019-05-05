package com.ybliu.breakpoint.util;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Cleaner;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created by linlinyeyu on 2019/4/29
 * @author linlinyeyu
 */
@Slf4j
public class FileUtil {
    private RandomAccessFile file;

    public FileUtil(String fileName, long startPos) throws IOException {
        file = new RandomAccessFile(fileName,"rw");
        file.seek(startPos);
    }

    public int write(byte[] data,int start,int len) {
        int res = -1;
        try {
            file.write(data,start,len);
            res = len;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void freeMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                try {
                    Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner",new Class[0]);
                    getCleanerMethod.setAccessible(true);
                    Cleaner cleaner = (Cleaner) getCleanerMethod.invoke(mappedByteBuffer,new Object[0]);
                    cleaner.clean();
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
