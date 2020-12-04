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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * RemoteException
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class RemoteException extends Exception {

	private static final long serialVersionUID = -509623868336336659L;
	private int errCode;
	private String subErrorCode;

	private String subErrorMsg;

	public String getSubErrorCode() {
		return subErrorCode;
	}

	public void setSubErrorCode(String subErrorCode) {
		this.subErrorCode = subErrorCode;
	}

	public String getSubErrorMsg() {
		return subErrorMsg;
	}

	public void setSubErrorMsg(String subErrorMsg) {
		this.subErrorMsg = subErrorMsg;
	}

	public int getErrCode() {
		return errCode;
	}

	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}

	public RemoteException(int errCode, String msg) {
		this(errCode, msg, null, null);
	}

	public RemoteException(String msg) {
		this(-1, msg, null, null);
	}

	public RemoteException(int errCode, String msg, String subErrorCode, String subErrorMsg) {
		super(msg);
		this.errCode = errCode;
		this.subErrorCode = subErrorCode;
		this.subErrorMsg = subErrorMsg;
	}

	public RemoteException(String subErrorCode, String subErrorMsg, Throwable e) {
		super(getStackTrace(e));
		this.subErrorCode = subErrorCode;
		this.subErrorMsg = subErrorMsg;
	}

	private static String getStackTrace(Throwable e) {
		String stackTrace = "";
		Writer writer = null;
		PrintWriter printWriter = null;
		if (e == null) {
			return stackTrace;
		}
		try {
			writer = new StringWriter();
			printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			stackTrace = writer.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (printWriter != null) {
				try {
					printWriter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		return stackTrace;
	}
}
