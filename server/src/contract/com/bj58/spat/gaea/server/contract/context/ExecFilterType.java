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
package com.bj58.spat.gaea.server.contract.context;

/**
 * 用来控制Filter的使用范围。
 * All表示任何场合都可以使用，比如request阶段、response阶段、connected阶段等。
 * RequestOnly表示只有在request阶段才能使用。
 * ResponseOnly表示只有在response阶段才能使用。
 * None表示任何场合都不能使用，相当于关闭。
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public enum ExecFilterType {

	None,
	
	RequestOnly,
	
	ResponseOnly,
	
	All
}
