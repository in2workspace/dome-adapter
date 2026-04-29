package es.altia.domeadapter.backend;


import es.altia.domeadapter.DomeAdapterApplication;
import org.springframework.boot.SpringApplication;

public class TestDomeAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.from(DomeAdapterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
