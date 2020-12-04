package com.bj58.spat.gaea.server.bootstrap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bj58.spat.gaea.server.bootstrap.Main;

public class MainTest {
	@Before
	public void init(){
		System.setProperty("user.dir","E:/GaeaStudy/gaea/bin");
	}
	@Test
	public void testMain() throws Exception {
		Main.main(new String[] { "-Dgaea.service.name=gaeaDemo" });
	}
}