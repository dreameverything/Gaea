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
package com.bj58.spat.gaea.server.deploy.bytecode;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import com.bj58.spat.gaea.server.contract.annotation.HttpPathParameter;
import com.bj58.spat.gaea.server.contract.annotation.HttpRequestMapping;

/**
 * <pre>
 * GaeaBinaryConvert
 * 类的定义，包括以ServiceContract注解标注的接口和以ServiceBehavior注解标注的Service实现类。
 * 包含了类的方法和参数的静态定义类MethodInfo和ParamInfo
 * ServiceContract注解如果defaultAll属性为true，则表示该接口以及所有父接口的方法都对外可以调用；否则，只是以注解OperationContract标注的方法才对外开放。
 *
 * 一个ClassInfo类包含一个MethodInfo的List列表，每个MethodInfo又包含一个ParamInfo数组，每个ParamInfo既包含了参数的名称，也包含了参数的值。
 * </pre>
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ClassInfo {
	/**
	 * 关联的Class类
	 */
	private Class<?> cls;
	/**
	 * 服务类中的方法定义列表
	 */
	private List<MethodInfo> methodList;
	/**
	 * 类型：类或者接口
	 */
	private ClassType classType;
	/**
	 * ServiceBehavior注解的Service实现类的名称
	 * 默认使用cls.getSimpleName()
	 */
	private String lookUP;

	public static class MethodInfo {
		/**
		 * 服务类中的方法
		 */
		private Method method;
		private ParamInfo[] paramInfoAry;
		private HttpRequestMapping httpRequestMapping;

		public Method getMethod() {
			return method;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		public void setHttpRequestMapping(HttpRequestMapping httpRequestMapping) {
			this.httpRequestMapping = httpRequestMapping;
		}

		public HttpRequestMapping getHttpRequestMapping() {
			return httpRequestMapping;
		}

		public void setParamInfoAry(ParamInfo[] paramInfoAry) {
			this.paramInfoAry = paramInfoAry;
		}

		public ParamInfo[] getParamInfoAry() {
			return paramInfoAry;
		}
	}

	public static class ParamInfo {
		/**
		 * 参数索引
		 */
		private int index;
		private Class<?> cls;
		/**
		 * 参数类型
		 */
		private Type type;
		/**
		 * 参数名称
		 */
		private String name;
		private String mapping;
		private HttpPathParameter httpPathParameter;

		public ParamInfo() {

		}

		/**
		 *
		 * @param index 参数的索引，从0开始计数
		 * @param cls 参数的Class
		 * @param type 参数的Type
		 * @param name 参数的名称
		 * @param mapping Http相关
		 * @param httpPathParameter Http相关
		 */
		public ParamInfo(int index, Class<?> cls, Type type, String name,
				String mapping, HttpPathParameter httpPathParameter) {
			super();
			this.index = index;
			this.cls = cls;
			this.type = type;
			this.name = name;
			this.mapping = mapping;
			this.httpPathParameter = httpPathParameter;
		}

		public int getIndex() {
			return index;
		}

		public Type getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setHttpPathParameter(HttpPathParameter httpPathParameter) {
			this.httpPathParameter = httpPathParameter;
		}

		public HttpPathParameter getHttpPathParameter() {
			return httpPathParameter;
		}

		public void setMapping(String mapping) {
			this.mapping = mapping;
		}

		public String getMapping() {
			return mapping;
		}

		public void setCls(Class<?> cls) {
			this.cls = cls;
		}

		public Class<?> getCls() {
			return cls;
		}
	}

	public enum ClassType {
		INTERFACE,

		CLASS
	}

	public Class<?> getCls() {
		return cls;
	}

	public List<MethodInfo> getMethodList() {
		return methodList;
	}

	public void setCls(Class<?> cls) {
		this.cls = cls;
	}

	public void setMethodList(List<MethodInfo> methodList) {
		this.methodList = methodList;
	}

	public void setClassType(ClassType classType) {
		this.classType = classType;
	}

	public ClassType getClassType() {
		return classType;
	}

	public void setLookUP(String lookUP) {
		this.lookUP = lookUP;
	}

	public String getLookUP() {
		return lookUP;
	}
}