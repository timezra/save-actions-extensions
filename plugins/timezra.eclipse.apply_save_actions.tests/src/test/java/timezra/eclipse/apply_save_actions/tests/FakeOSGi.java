package timezra.eclipse.apply_save_actions.tests;

import java.util.Collections;

import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class FakeOSGi {

	public static Bundle start() {
		final Framework framework = new EquinoxFactory().newFramework(Collections.<String, String>emptyMap());
		try {
			framework.init();
			return framework;
		} catch (final BundleException e) {
			throw new RuntimeException(e);
		}
	}
}