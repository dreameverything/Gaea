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
import com.bj58.spat.gaea.serializer.serializer.SerializerBase;

import java.math.BigDecimal;

/**
 * DecimalSerializer
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
class DecimalSerializer extends SerializerBase {

	@Override
	public void WriteObject(Object obj, GaeaOutStream outStream) throws Exception {
		SerializerFactory.GetSerializer(String.class).WriteObject(obj.toString(), outStream);
	}

	/**
	 * <pre>
	 *     BigDecimal传输过程中是转换成字符串了
	 *
	 * </pre>
	 * @param inStream 包含了Netty中解析出来的字节数组
	 * @param defType 字节数组对应的类型
	 * @return
	 * @throws Exception
	 */
	@Override
	public Object ReadObject(GaeaInStream inStream, Class defType) throws Exception {
		Object value = SerializerFactory.GetSerializer(String.class).ReadObject(inStream, String.class);
		if (value != null) {
			return new BigDecimal(value.toString());
		}
		return BigDecimal.ZERO;
	}
}
