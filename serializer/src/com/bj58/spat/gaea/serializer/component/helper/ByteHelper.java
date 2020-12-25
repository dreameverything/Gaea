/*
 *  Copyright Beijing 58 Information Technology Co.,Ltd.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package com.bj58.spat.gaea.serializer.component.helper;

import com.bj58.spat.gaea.serializer.component.exception.OutOfRangeException;

/**
 * <pre>
 *   最低有效位（Least Significant Bit，lsb）是指指一个二进制数字中的第0位（即最低位），与之相反的称之为最高有效位。在大端序中，lsb指最右边的位。
 *   小端序（小尾序）:最低有效位在最高有效位的前面，或者是最低位字节存储在内存的最低地址处。
 *   大端序（大尾序）:最低有效位在最高有效位的后面，或者是最高位字节存储在内存的最低地址处。
 *
 *   小端序例子：
 *   32bit int = 0A0B0C0D
 *   地址：低位==========>高位
 *        0D 0C 0B 0A
 *
 *   大端序的例子：
 *   32bit int = 0A0B0C0D
 *   地址：低位=========>高位
 *         0A 0B 0C 0D
 *
 *   可以理解为，大端序与字节序是一致的，但是小端序是逆序的。
 *   从地址由低到高看成是低位是尾巴，高位是头，那么尾巴处的数据是最低有效位的字节，那就是小尾序（小端序），如果
 *   尾巴处的数据是最高有效位的字节，那就是大尾序（大端序）
 *   </pre>
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ByteHelper {

    public static short ToInt16(byte[] buffer) throws OutOfRangeException {
        if (buffer.length < 2) {
            throw new OutOfRangeException();
        }
        short int16 = 0;
        int16 = (short) (buffer[0] & 0xff);
        int16 |= ((short) buffer[1] << 8) & 0xff00;
        return int16;
    }

    /**
     * <pre>
     * 将字节数组转为int类型，byte[0]为最低位字节，byte[4]为最高位字节。
     * 具体转换过程如下：
     * 假设参数buffer[4]的每个字节为：
     * 地址：(低位)==============>(高位)
     *   -------------------------------
     *   index -  [0]   [1]  [2]   [3]
     *   -------------------------------
     *    byte - 0x01 0x02 0x03 0x04
     *   -------------------------------
     *
     * 转换的过程为：
     * 1. 0x01 & 0xff =0x01
     * 2. 0x02<<8 = 0x0200 & 0xff00 = 0x02ff | 0x01 =0x0201
     * 3. 0x03<<16 = 0x030000 & 0xff0000 = 0x030000 | 0x0201 = 0x030201
     * 4. 0x04<<24 = 0x04000000 & 0xff000000 = 0x04000000 | 0x030201 = 0x04030201
     *
     * 转后后的结果为：
     * 地址：(低位)==============>(高位)
     *   -------------------------------
     *   index -  [0]   [1]  [2]   [3]
     *   -------------------------------
     *    byte - 0x04 0x03 0x02 0x01
     *   -------------------------------
     * </pre>
     * @param buffer
     * @return
     * @throws OutOfRangeException
     */
    public static int ToInt32(byte[] buffer) throws OutOfRangeException {
        if (buffer.length < 4) {
            throw new OutOfRangeException();
        }
        int int32 = 0;
        int32 = buffer[0] & 0xff;
        int32 |= ((int) buffer[1] << 8) & 0xff00;
        int32 |= ((int) buffer[2] << 16) & 0xff0000;
        int32 |= ((int) buffer[3] << 24) & 0xff000000;
        return int32;
    }

    /**
     * <pre>
     *     (接着 GetBytesFromInt64(long value) 方法的注释继续画流程图)
     *
     *      index:         0      1       2       3       4       5       6        7
     *      发送方的数据：byte[7] byte[6] byte[5] byte[4] byte[3] byte[2] byte[1] byte[0]
     *
     *      收到的字节的顺序与发送方正好相反，需要改变字节序，改变后变为：
     *      index:         0      1       2       3       4       5       6        7
     *      改变后的数据：byte[0] byte[1] byte[2] byte[3] byte[4] byte[5] byte[6] byte[7]
     *
     *      此时就与发送方的数据顺序一致了。
     *
     * </pre>
     * @param buffer
     * @return
     * @throws OutOfRangeException
     */
    public static long ToInt64(byte[] buffer) throws OutOfRangeException {
        if (buffer.length < 8) {
            throw new OutOfRangeException();
        }
        long int64 = 0;
        int64 = buffer[0] & 0xffL;
        int64 |= ((long) buffer[1] << 8) & 0xff00L;
        int64 |= ((long) buffer[2] << 16) & 0xff0000L;
        int64 |= ((long) buffer[3] << 24) & 0xff000000L;
        int64 |= ((long) buffer[4] << 32) & 0xff00000000L;
        int64 |= ((long) buffer[5] << 40) & 0xff0000000000L;
        int64 |= ((long) buffer[6] << 48) & 0xff000000000000L;
        int64 |= ((long) buffer[7] << 56);
        return int64;
    }

    public static byte[] GetBytesFromInt16(short value) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >> 8);
        return buffer;
    }

    public static byte[] GetBytesFromInt32(int value) {
        byte[] buffer = new byte[4];
        for (int i = 0; i < 4; i++) {
            buffer[i] = (byte) (value >> (8 * i));
        }
        return buffer;
    }

    /**
     * <pre>
     *     byte[0] byte[1] byte[2] byte[3] byte[4] byte[5] byte[6] byte[7]
     *       To:
     *     byte[7] byte[6] byte[5] byte[4] byte[3] byte[2] byte[1] byte[0]
     *
     *     内存：                   网络传输：-------->时间线              接收方
     *     addr0 : byte[7]                               ---->first
     *     addr1 : byte[6]                            ---->second
     *     addr2 : byte[5]                         ---->
     *     addr3 : byte[4]                      ---->
     *     addr4 : byte[3]                   ---->
     *     addr5 : byte[2]                ---->
     *     addr6 : byte[1]             ---->
     *     addr7 : byte[0]          ---->end
     *
     *     ToInt64(byte[])方法是本方法的逆向操作
     * </pre>
     * @param value
     * @return
     */
    public static byte[] GetBytesFromInt64(long value) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (value >> (8 * i));
        }
        return buffer;
    }

    public static byte[] GetBytesFromChar(char ch) {
        int temp = (int) ch;
        byte[] b = new byte[2];
        for (int i = b.length - 1; i > -1; i--) {
        	//将最高位保存在最低位
            b[i] = new Integer(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
        return b;
    }

    public static char getCharFromBytes(byte[] b) {
        int s = 0;
        if (b[0] > 0) {
            s += b[0];
        } else {
            s += 256 + b[0];
        }
        s *= 256;
        if (b[1] > 0) {
            s += b[1];
        } else {
            s += 256 + b[1];
        }
        char ch = (char) s;
        return ch;
    }
}
