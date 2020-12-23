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
package com.bj58.spat.gaea.protocol.serializer;

import com.bj58.spat.gaea.serializer.serializer.Serializer;

/**
 * GaeaSerialize
 *
 * @author Service Platform Architecture Team (spat@58.com)
 */
class GaeaSerialize extends SerializeBase {
    /**
     * 初始化Serializer，改类包含了Gaea序列化支持的所有的类型和自定义的Class类型
     */
    private static final Serializer SERIALIZER = new Serializer();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        return SERIALIZER.Serialize(obj);
    }

    @Override
    public Object deserialize(byte[] data, Class<?> cls) throws Exception {
        return SERIALIZER.Derialize(data, cls);
    }
}