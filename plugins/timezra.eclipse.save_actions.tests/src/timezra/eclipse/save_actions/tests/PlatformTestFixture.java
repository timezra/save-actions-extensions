package timezra.eclipse.save_actions.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Stack;

import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class PlatformTestFixture {

	private BundleContext theContext;

	private final Stack<BundleActivator> toStop;
	private final Collection<ServiceReference> toUnregister;
	private final Collection<IExtensionPoint> toRemove;
	private InternalPlatform internalPlatform;

	public PlatformTestFixture() {
		toStop = new Stack<BundleActivator>();
		toUnregister = new ArrayList<ServiceReference>();
		toRemove = new ArrayList<IExtensionPoint>();
	}

	public PlatformTestFixture start() {
		try {
			doStart();
		} catch (final Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Unable to start the fake platform", e);
		}
		return this;
	}

	private void doStart() throws Exception {
		theContext = FakeOSGi.start().getBundleContext();
		(internalPlatform = InternalPlatform.getDefault()).start(theContext);
		start(new org.eclipse.core.internal.runtime.Activator());
		start(new org.eclipse.core.internal.registry.osgi.Activator());
		start(new org.eclipse.core.internal.content.Activator());

		addTheExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS, new RegistryContributor("actualID",
				"actualName", null, null));

		registerTheService(IContentTypeManager.class.getName(), ContentTypeManager.getInstance());
		registerTheService(IPreferencesService.class.getName(), PreferencesService.getDefault());

		start(new ResourcesPlugin());
	}

	private void start(final BundleActivator b) throws Exception {
		b.start(theContext);
		toStop.add(b);
	}

	private void addTheExtensionPoint(final String elementName, final String xpt, final IContributor contributor) {
		final ExtensionRegistry extensionRegistry = (ExtensionRegistry) Platform.getExtensionRegistry();
		extensionRegistry.addExtensionPoint(String.format("%s.%s", elementName, xpt), contributor, false, xpt, null,
				extensionRegistry.getTemporaryUserToken());
		toRemove.add(extensionRegistry.getExtensionPoint(elementName, xpt));
	}

	private void registerTheService(final String name, final Object service) {
		toUnregister.add(theContext.registerService(name, service, new Hashtable<Object, Object>()).getReference());
	}

	public void dispose() {
		try {
			doDispose();
		} catch (final Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Unable to stop the fake platform", e);
		}
	}

	private void doDispose() throws Exception {
		for (final IExtensionPoint xpt : toRemove) {
			final ExtensionRegistry extensionRegistry = (ExtensionRegistry) Platform.getExtensionRegistry();
			extensionRegistry.removeExtensionPoint(xpt, extensionRegistry.getTemporaryUserToken());
		}
		for (final ServiceReference r : toUnregister) {
			theContext.ungetService(r);
		}
		while (!toStop.isEmpty()) {
			toStop.pop().stop(theContext);
		}
		if (internalPlatform != null) {
			internalPlatform.stop(theContext);
		}
	}
}
