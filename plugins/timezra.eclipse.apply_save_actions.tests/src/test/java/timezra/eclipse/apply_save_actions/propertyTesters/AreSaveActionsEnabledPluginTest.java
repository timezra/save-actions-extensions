package timezra.eclipse.apply_save_actions.propertyTesters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.ui.JavaUI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import timezra.eclipse.apply_save_actions.Constants;
import timezra.eclipse.apply_save_actions.tests.ModifiesSaveActionsPreferences;
import timezra.eclipse.apply_save_actions.tests.ModifiesSaveActionsPreferencesRule;

public class AreSaveActionsEnabledPluginTest {

	@Rule
	public final MethodRule rule = new ModifiesSaveActionsPreferencesRule();

	@Test
	@ModifiesSaveActionsPreferences
	public void saveActionsAreEnabledByAnEclipsePreference() {
		setPreference(true);
		assertTrue(new AreSaveActionsEnabled().test(null, null, null, null));
	}

	@Test
	@ModifiesSaveActionsPreferences
	public void saveActionsAreDisabledByAnEclipsePreference() {
		setPreference(false);
		assertFalse(new AreSaveActionsEnabled().test(null, null, null, null));
	}

	private void setPreference(final boolean b) {
		InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).putBoolean(Constants.PERFORM_SAVE_ACTIONS_PREFERENCE, b);
	}
}
