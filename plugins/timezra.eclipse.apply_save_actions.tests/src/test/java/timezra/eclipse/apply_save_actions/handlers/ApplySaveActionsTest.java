package timezra.eclipse.apply_save_actions.handlers;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import timezra.eclipse.apply_save_actions.tests.PlatformTestFixture;

public class ApplySaveActionsTest {

	private static final String WORKSPACE = "../../plugins/timezra.eclipse.apply_save_actions.test/target/workspace";
	private PlatformTestFixture testFixture;

	@Before
	public void setUp() {
		testFixture = new PlatformTestFixture().start();
	}

	@After
	public void tearDown() {
		testFixture.dispose();
	}

	@SuppressWarnings("restriction")
	@Test
	public void aJavaFileCanBeReformatted() throws Exception {
		final Workspace workspace = new Workspace();
		final String filename = "AJavaFile.java";
		final Resource aJavaFile = workspace.newResource(Path
				.fromPortableString(String.format("%s/%s", WORKSPACE, filename)), IResource.FILE);

		final ApplySaveActions command = new ApplySaveActions();
		final EvaluationContext context = new EvaluationContext(null, new Object());
		context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, new StructuredSelection(aJavaFile));
		final ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, context);
		command.execute(event);
	}

	public static final class SimpleAdapterManager implements IAdapterManager {

		private final Map<IAdapterFactory, Collection<Class>> factories2adaptables;

		public SimpleAdapterManager() {
			factories2adaptables = new HashMap<IAdapterFactory, Collection<Class>>();
		}

		private Collection<IAdapterFactory> factories4adaptable(final Class adaptable) {
			final Collection<IAdapterFactory> factories4adaptable = new HashSet<IAdapterFactory>();
			for (final Entry<IAdapterFactory, Collection<Class>> e : factories2adaptables.entrySet()) {
				if (e.getValue().contains(adaptable)) {
					factories4adaptable.add(e.getKey());
				}
			}
			return factories4adaptable;
		}

		public String[] computeAdapterTypes(final Class adaptableClass) {
			final Collection<String> adapterTypes = new HashSet<String>();
			for (final Class adaptable : computeClassOrder(adaptableClass)) {
				for (final IAdapterFactory f : factories4adaptable(adaptable)) {
					for (final Class c : f.getAdapterList()) {
						adapterTypes.add(c.getName());
					}
				}
			}
			return adapterTypes.toArray(new String[adapterTypes.size()]);
		}

		private void classHierarchy(final List<Class> acc, final Class c) {
			if (c == null) {
				return;
			}
			acc.add(c);
			classHierarchy(acc, c.getSuperclass());
		}

		private void interfaceHierarchy(final List<Class> acc, final Class... is) {
			if (is.length == 0) {
				return;
			}
			acc.addAll(asList(is));
			final Collection<Class> superInterfaces = new ArrayList<Class>();
			for (final Class i : is) {
				superInterfaces.addAll(asList(i.getInterfaces()));
			}
			interfaceHierarchy(acc, superInterfaces.toArray(new Class[superInterfaces.size()]));
		}

		public Class[] computeClassOrder(final Class clazz) {
			final List<Class> acc = new ArrayList<Class>();
			classHierarchy(acc, clazz);
			interfaceHierarchy(acc, clazz.getInterfaces());
			return acc.toArray(new Class[acc.size()]);
		}

		public Object getAdapter(final Object adaptable, final Class adapterType) {
			return getAdapter(adaptable, adapterType.getName());
		}

		public Object getAdapter(final Object adaptable, final String adapterTypeName) {
			return loadAdapter(adaptable, adapterTypeName);
		}

		public boolean hasAdapter(final Object adaptable, final String adapterTypeName) {
			return loadAdapter(adaptable, adapterTypeName) != null;
		}

		public Object loadAdapter(final Object adaptable, final String adapterTypeName) {
			return null;
		}

		public int queryAdapter(final Object o, final String adapterTypeName) {
			return hasAdapter(o, adapterTypeName) ? LOADED : NONE;
		}

		public void registerAdapters(final IAdapterFactory factory, final Class adaptable) {
			if (!factories2adaptables.containsKey(factory)) {
				factories2adaptables.put(factory, new HashSet<Class>());
			}
			factories2adaptables.get(adaptable).add(adaptable);
		}

		public void unregisterAdapters(final IAdapterFactory factory) {
			factories2adaptables.remove(factory);
		}

		public void unregisterAdapters(final IAdapterFactory factory, final Class adaptable) {
			if (factories2adaptables.containsKey(factory)) {
				final Collection<Class> adaptables = factories2adaptables.get(factory);
				adaptables.remove(adaptable);
				if (adaptables.isEmpty()) {
					factories2adaptables.remove(factory);
				}
			}
		}
	}
}
