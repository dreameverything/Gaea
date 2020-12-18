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
package com.bj58.spat.gaea.protocol.sfp.enumeration;

/**
 * <pre>
 * 序列化协议
 * 只实现了GAEABinary，参见创建协议的工厂类SerializeBase
 * 其他协议尚未实现
 * </pre>
 *
 * @author Service Platform Architecture Team (spat@58.com)
 */
public enum SerializeType {

    JSON(1),
    JAVABinary(2),
    XML(3),
    GAEABinary(4);
    
    private final int num;

    public int getNum() {
        return num;
    }

    private SerializeType(int num) {
        this.num = num;
    }

    public static SerializeType getSerializeType(int num) {
        for (SerializeType type : SerializeType.values()) {
            if (type.getNum() == num) {
                return type;
            }
        }
        return null;
    }
}
