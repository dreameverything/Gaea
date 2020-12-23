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
package com.bj58.spat.gaea.protocol.sdp;

import java.util.List;

import com.bj58.spat.gaea.protocol.utility.KeyValuePair;
import com.bj58.spat.gaea.serializer.component.annotation.GaeaMember;
import com.bj58.spat.gaea.serializer.component.annotation.GaeaSerializable;

/**
 * <pre>
 * RequestProtocol包含GaeaSerializable注解，注解的name为"RequestProtocol",
 * 会被com.bj58.spat.gaea.serializer.component.TypeMap作为自定义的类型加载到内存中。
 *
 * 包名sdp = Simple Data Protocol ，即：简单的数据协议
 * </pre>
 * @author Service Platform Architecture Team (spat@58.com)
 */
@GaeaSerializable(name = "RequestProtocol")
public class RequestProtocol {
	/**
	 * 要找的代理类
	 */
	@GaeaMember
	private String lookup;
	/**
	 * 要调用的方法名称
	 */
	@GaeaMember
	private String methodName;
	/**
	 * 参数列表
	 */
	@GaeaMember
	private List<KeyValuePair> paraKVList;

	public RequestProtocol() {
	}

	public RequestProtocol(String lookup, String methodName,
			List<KeyValuePair> paraKVList) {
		this.lookup = lookup;
		this.methodName = methodName;
		this.paraKVList = paraKVList;
	}

	public String getLookup() {
		return lookup;
	}

	public void setLookup(String lookup) {
		this.lookup = lookup;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public List<KeyValuePair> getParaKVList() {
		return paraKVList;
	}

	public void setParaKVList(List<KeyValuePair> paraKVList) {
		this.paraKVList = paraKVList;
	}
}
