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

import com.bj58.spat.gaea.serializer.component.exception.DisallowedSerializeException;
import com.bj58.spat.gaea.serializer.component.helper.TypeHelper;
import com.bj58.spat.gaea.serializer.serializer.ArraySerializer;
import com.bj58.spat.gaea.serializer.serializer.BooleanSerializer;
import com.bj58.spat.gaea.serializer.serializer.ByteSerializer;
import com.bj58.spat.gaea.serializer.serializer.CharSerializer;
import com.bj58.spat.gaea.serializer.serializer.DateTimeSerializer;
import com.bj58.spat.gaea.serializer.serializer.DecimalSerializer;
import com.bj58.spat.gaea.serializer.serializer.DoubleSerializer;
import com.bj58.spat.gaea.serializer.serializer.EnumSerializer;
import com.bj58.spat.gaea.serializer.serializer.FloatSerializer;
import com.bj58.spat.gaea.serializer.serializer.Int16Serializer;
import com.bj58.spat.gaea.serializer.serializer.Int32Serializer;
import com.bj58.spat.gaea.serializer.serializer.Int64Serializer;
import com.bj58.spat.gaea.serializer.serializer.KeyValueSerializer;
import com.bj58.spat.gaea.serializer.serializer.ListSerializer;
import com.bj58.spat.gaea.serializer.serializer.MapSerializer;
import com.bj58.spat.gaea.serializer.serializer.NullSerializer;
import com.bj58.spat.gaea.serializer.serializer.ObjectSerializer;
import com.bj58.spat.gaea.serializer.serializer.SerializerBase;
import com.bj58.spat.gaea.serializer.serializer.StringSerializer;

/**
 * <pre>
 * SerializerFactory序列化工厂类
 *
 * </pre>
 * @author Service Platform Architecture Team (spat@58.com)
 */
class SerializerFactory {

    private final static SerializerBase arraySerializer = new ArraySerializer();
    private final static SerializerBase boolSerializer = new BooleanSerializer();
    private final static SerializerBase byteSerializer = new ByteSerializer();
    private final static SerializerBase charSerializer = new CharSerializer();
    private final static SerializerBase dateTimeSerializer = new DateTimeSerializer();
    private final static SerializerBase decimalSerializer = new DecimalSerializer();
    private final static SerializerBase doubleSerializer = new DoubleSerializer();
    private final static SerializerBase enumSerializer = new EnumSerializer();
    private final static SerializerBase floatSerializer = new FloatSerializer();
    private final static SerializerBase int16Serializer = new Int16Serializer();
    private final static SerializerBase int32Serializer = new Int32Serializer();
    private final static SerializerBase int64Serializer = new Int64Serializer();
    private final static SerializerBase keyValueSerializer = new KeyValueSerializer();
    private final static SerializerBase listSerializer = new ListSerializer();
    private final static SerializerBase mapSerializer = new MapSerializer();
    private final static SerializerBase nullSerializer = new NullSerializer();
    private final static SerializerBase objectSerializer = new ObjectSerializer();
    private final static SerializerBase stringSerializer = new StringSerializer();

    /**
     * 根据type获取到对应的序列化类
     * @param type
     * @return
     * @throws ClassNotFoundException
     * @throws DisallowedSerializeException
     */
    public static SerializerBase GetSerializer(Class<?> type) throws ClassNotFoundException, DisallowedSerializeException {
        if (type == null) {
            return nullSerializer;
        } else if (type.isEnum()) {
            return enumSerializer;
        }
        int typeId = TypeHelper.GetTypeId(type);
        SerializerBase serializer = null;
        switch (typeId) {
            case 0://未知
            case 1://DBNull
                serializer = nullSerializer;
                break;
            case 2://Object
                serializer = objectSerializer;
                break;
            case 3:
                serializer = boolSerializer;
                break;
            case 4:
                serializer = charSerializer;
                break;
            case 5://Byte
            case 6://之前的版本有6，这里空出来是为了兼容旧版本
                serializer = byteSerializer;
                break;
            case 7://Short
            case 8://未知
                serializer = int16Serializer;
                break;
            case 9://Integer
            case 10://未知
                serializer = int32Serializer;
                break;
            case 11://Long
            case 12://未知
                serializer = int64Serializer;
                break;
            case 13://Float
                serializer = floatSerializer;
                break;
            case 14:
                serializer = doubleSerializer;
                break;
            case 15:
                serializer = decimalSerializer;
                break;
            case 16://Date, java.sql.Date, java.sql.Time, java.sql.Timestamp
                serializer = dateTimeSerializer;
                break;
            case 18:
                serializer = stringSerializer;
                break;
            case 19://List
            case 20://之前的版本有20，这里空出来是为了兼容旧版本
            case 21://之前的版本有21，这里空出来是为了兼容旧版本
                serializer = listSerializer;
                break;
            case 22://GKeyValuePair
                serializer = keyValueSerializer;
                break;
            case 23:
                serializer = arraySerializer;
                break;
            case 24://Map
            case 25://之前的版本有25，这里空出来是为了兼容旧版本
                serializer = mapSerializer;
                break;
            default:
                serializer = objectSerializer; //自定义的序列化类（例如:RequestProtocol、ResponseProtocol等）
        }
        return serializer;
    }
}
