package devmalik19.litrarr;

import devmalik19.litrarr.helper.StartupTasks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AppTests
{
	@MockitoBean
	private StartupTasks startupTasks;

	@Test
	void contextLoads()
	{
	}
}
