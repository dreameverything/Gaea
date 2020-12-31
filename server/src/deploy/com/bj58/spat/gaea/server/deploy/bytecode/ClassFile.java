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

/**
 * 字节码类文件
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ClassFile {
	/**
	 * 类的名称
	 */
	private String clsName;
	/**
	 * 类文件的字节数组
	 */
	private byte[] clsByte;
	
	
	public ClassFile(String clsName, byte[] clsByte){
		this.setClsName(clsName);
		this.setClsByte(clsByte);
	}
	
	
	public String getClsName() {
		return clsName;
	}
	public void setClsName(String clsName) {
		this.clsName = clsName;
	}
	public byte[] getClsByte() {
		return clsByte;
	}
	public void setClsByte(byte[] clsByte) {
		this.clsByte = clsByte;
	}
}
