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
package com.bj58.spat.gaea.server.contract.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bj58.spat.gaea.server.contract.annotation.AnnotationUtil;

/**
 * a annotation for mark implement class
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceBehavior {
	/**
	 * 通过lookUP设定的名称，可以找到代理类。如果注解没有设定looUP，即使用默认值AnnotationUtil.DEFAULT_VALUE，那么
	 * 类的lookUP的值会默认为类的名称
	 * @return
	 */
	public String lookUP() default AnnotationUtil.DEFAULT_VALUE;
}