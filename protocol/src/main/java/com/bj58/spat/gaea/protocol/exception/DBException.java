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
package com.bj58.spat.gaea.protocol.exception;

/**
 * DBException
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class DBException extends RemoteException {

	private static final long serialVersionUID = 5349859018989590802L;

	public DBException() {
		super("服务器端数据库错误!");
	}

	public DBException(String message) {
		this(message, null, null);
	}

	public DBException(String message, String subErrorCode, String subErrorMsg) {
		super(message);
		this.setErrCode(ReturnType.DB);
		this.setSubErrorCode(subErrorCode);
		this.setSubErrorMsg(subErrorMsg);
	}
}
