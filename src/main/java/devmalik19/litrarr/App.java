package devmalik19.litrarr;

import devmalik19.litrarr.lifecycle.DevSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class App
{
	public static void main(String[] args)
	{
		SpringApplication application = new SpringApplication(App.class);
		// Runs before the datasource connects. Does nothing unless the "dev" profile is active.
		application.addListeners(new DevSetup());
		application.run(args);
	}
}
