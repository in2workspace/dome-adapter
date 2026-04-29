package es.altia.domeadapter;

import org.springframework.boot.SpringApplication;

public class TestDomeAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.from(DomeAdapterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
