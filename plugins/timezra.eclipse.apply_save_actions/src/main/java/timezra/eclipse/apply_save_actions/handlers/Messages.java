package timezra.eclipse.apply_save_actions.handlers;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "timezra.eclipse.apply_save_actions.handlers.messages"; //$NON-NLS-1$
	public static String APPLY_SAVE_ACTIONS_BEGIN_TASK;
	public static String APPLY_SAVE_ACTIONS_UNEXPECTED_ERROR;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
