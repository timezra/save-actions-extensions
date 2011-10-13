package timezra.eclipse.apply_save_actions.propertyTesters;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.ui.JavaUI;

import timezra.eclipse.apply_save_actions.Constants;

public class AreSaveActionsEnabled extends PropertyTester {

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		return InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).getBoolean(Constants.PERFORM_SAVE_ACTIONS_PREFERENCE,
				false);
	}
}
