package timezra.eclipse.apply_save_actions.tests;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.ui.JavaUI;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import timezra.eclipse.apply_save_actions.Constants;

public class ModifiesSaveActionsPreferencesRule implements MethodRule {

	// based on tutorial: http://blog.mycila.com/2009/11/writing-your-own-junit-extensions-using.html
	@Override
	public Statement apply(final Statement statement, final FrameworkMethod method, final Object target) {
		if (method.getAnnotation(ModifiesSaveActionsPreferences.class) == null) {
			return statement;
		}
		return new Statement() {
			@SuppressWarnings("restriction")
			@Override
			public void evaluate() throws Throwable {
				final boolean performSaveActionsPreference = InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN)
						.getBoolean(Constants.PERFORM_SAVE_ACTIONS_PREFERENCE, false);
				try {
					statement.evaluate();
				} finally {
					InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).putBoolean(
							Constants.PERFORM_SAVE_ACTIONS_PREFERENCE, performSaveActionsPreference);
					org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil
							.saveSaveParticipantOptions(
									InstanceScope.INSTANCE,
									org.eclipse.jdt.internal.ui.JavaPlugin
											.getDefault()
											.getCleanUpRegistry()
											.getDefaultOptions(
													org.eclipse.jdt.internal.corext.fix.CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS)
											.getMap());
				}
			}
		};
	}
}
