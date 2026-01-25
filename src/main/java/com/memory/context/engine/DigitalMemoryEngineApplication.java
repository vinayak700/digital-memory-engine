package com.memory.context.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import java.net.URI;

@SpringBootApplication
public class DigitalMemoryEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalMemoryEngineApplication.class, args);
//		Jedis jedis = new Jedis(URI.create("rediss://default:AXMPAAIncDIxMTg5OGQwMWFkYmY0MTdmODhkNjhjOWUwZjMwOTQyN3AyMjk0NTU@funny-pipefish-29455.upstash.io:6379"));
//		jedis.set("foo", "bar");
//		String value = jedis.get("foo");
//		System.out.println(value);
	}

}
