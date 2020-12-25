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
package com.bj58.spat.gaea.serializer.serializer;

import com.bj58.spat.gaea.serializer.component.GaeaInStream;
import com.bj58.spat.gaea.serializer.component.GaeaOutStream;

/**
 * <pre>
 * 序列化的抽象类
 * </pre>
 * @author Service Platform Architecture Team (spat@58.com)
 */
abstract class SerializerBase {

    public abstract void WriteObject(Object obj, GaeaOutStream outStream) throws Exception;

    /**
     * <pre>
     * 解析的过程很像是拨开洋葱的过程，
     * 最外一层：( com.bj58.spat.gaea.protocol.sdp.RequestProtocol      )
     *     里边一层：(         成员 String lookup                    )
     *          里边一层：(     成员 String methodName            )
     *              里边一层：( List<KeyValuePair> paraKVList )
     *                      里边一层：(   KeyValuePair    )
     *                           ... ...
     *
     * 每一层的字节结构是：
     * --->||____|_|____|____|______d个字节_______|  --->  |____|_|____|____|______d个字节_______||  --->
     *        a   b  c    d         e                       a   b   c    d         e
     *       int    int  int      byte[d]                  int     int  int        byte[d]
     *      type ref hash len       data                  type ref hash len        data
     *
     * a是整数(4个字节)，表示类型的id，需要转换为正确的字节序
     * b是1个字节，表示isRef，如果为true，则直接从缓存中获取数据
     * c是整数(4个字节)，如果isRef=true，则c表示的是hashcode值
     * d是整数(4个字节)，表示的是该数据的长度
     * e是数据，数据的长度由d来设定。
     *
     * 以上形式循环往复，构成了一个整体的字节结构。
     * </pre>
     * @param inStream 包含了Netty中解析出来的字节数组
     * @param defType 字节数组对应的类型
     * @return 反序列化后的对象
     * @throws Exception
     */
    public abstract Object ReadObject(GaeaInStream inStream, Class defType) throws Exception;
}
