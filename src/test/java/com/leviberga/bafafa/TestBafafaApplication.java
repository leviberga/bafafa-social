package com.leviberga.bafafa;

import org.springframework.boot.SpringApplication;

public class TestBafafaApplication {

	public static void main(String[] args) {
		SpringApplication.from(BafafaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
