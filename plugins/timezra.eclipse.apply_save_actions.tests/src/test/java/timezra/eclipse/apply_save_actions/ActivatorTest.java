package timezra.eclipse.apply_save_actions;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ActivatorTest {

	@Test
	public void thePluginShouldBeActivated() {
		assertNotNull(Activator.getDefault());
	}
}
