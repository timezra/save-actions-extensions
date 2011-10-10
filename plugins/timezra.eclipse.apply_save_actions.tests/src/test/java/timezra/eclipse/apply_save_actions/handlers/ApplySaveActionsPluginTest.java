package timezra.eclipse.apply_save_actions.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import timezra.eclipse.apply_save_actions.Constants;
import timezra.eclipse.apply_save_actions.tests.ModifiesSaveActionsPreferences;
import timezra.eclipse.apply_save_actions.tests.ModifiesSaveActionsPreferencesRule;

public class ApplySaveActionsPluginTest {

	private static final String SOURCE_FOLDER = "src/test/java";

	private static final String EOL = System.getProperty("line.separator");

	private static final IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	private static final String TEST_CLASS_BEFORE_SAVE_ACTIONS = "package timezra.eclipse.apply_save_actions;import java.util.*;class TestClass{private List<TestClass> testClasses;TestClass(List<TestClass> testClasses){this.testClasses=testClasses;}}"
			+ EOL;

	private static final String TEST_CLASS_AFTER_SAVE_ACTIONS = "package timezra.eclipse.apply_save_actions;" + EOL + //
			EOL + //
			"import java.util.List;" + EOL + //
			EOL + //
			"class TestClass {" + EOL + //
			"	private final List<TestClass> testClasses;" + EOL + //
			EOL + //
			"	TestClass(List<TestClass> testClasses) {" + EOL + //
			"		this.testClasses = testClasses;" + EOL + //
			"	}" + EOL + //
			"}" + EOL;

	@Rule
	public final MethodRule rule = new ModifiesSaveActionsPreferencesRule();

	private IProject aJavaProject;
	private IFolder aJavaPackage;
	private IFile aJavaFile;

	private IFolder aJavaSourceFolder;

	@Before
	public void setUp() throws CoreException {
		aJavaProject = createAJavaProject("a_java_project");
		aJavaSourceFolder = createASourceFolder(SOURCE_FOLDER);
		aJavaPackage = createAPackage(aJavaSourceFolder, "timezra/eclipse/apply_save_actions");
		aJavaFile = createAJavaFile(aJavaPackage, "TestClass.java");
	}

	@After
	public void tearDown() throws CoreException {
		aJavaProject.delete(true, NULL_PROGRESS_MONITOR);
	}

	@Test
	public void theCurrentSelectionMustBeStructured() throws ExecutionException {
		final ApplySaveActions command = new ApplySaveActions();
		final EvaluationContext context = new EvaluationContext(null, new Object());
		context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, new TextSelection(0, 100));
		final ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, context);
		assertNull(command.execute(event));
	}

	@Test
	@ModifiesSaveActionsPreferences
	public void aJavaFileCanBeReformatted() throws ExecutionException, CoreException, IOException {
		enableJavaSaveActions();

		applySaveActions(JavaCore.create(aJavaFile));

		verifyThatSaveActionsHaveBeenApplied(aJavaFile);
	}

	@Test
	@ModifiesSaveActionsPreferences
	public void aJavaPackageCanBeReformatted() throws ExecutionException, CoreException, IOException {
		enableJavaSaveActions();

		applySaveActions(JavaCore.create(aJavaPackage));

		verifyThatSaveActionsHaveBeenApplied(aJavaFile);
	}

	@Test
	@ModifiesSaveActionsPreferences
	public void aJavaSourceFolderCanBeReformatted() throws ExecutionException, CoreException, IOException {
		enableJavaSaveActions();

		applySaveActions(JavaCore.create(aJavaSourceFolder));

		verifyThatSaveActionsHaveBeenApplied(aJavaFile);
	}

	@Test
	@ModifiesSaveActionsPreferences
	public void aJavaProjectCanBeReformatted() throws ExecutionException, CoreException {
		enableJavaSaveActions();

		applySaveActions(JavaCore.create(aJavaProject));

		verifyThatSaveActionsHaveBeenApplied(aJavaFile);
	}

	private void applySaveActions(final Object selection) throws ExecutionException {
		final ApplySaveActions command = new ApplySaveActions();
		final EvaluationContext context = new EvaluationContext(null, new Object());
		context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, new StructuredSelection(selection));
		final ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, context);
		command.execute(event);
	}

	// contains a beaut that turns a stream into a String without using IoUtils:
	// http://stackoverflow.com/questions/309424/in-java-how-do-a-read-convert-an-inputstream-in-to-a-string
	private void verifyThatSaveActionsHaveBeenApplied(final IFile aJavaFile) throws CoreException {
		final String actualContents;
		final Scanner scanner = new Scanner(aJavaFile.getContents());
		try {
			actualContents = scanner.useDelimiter("\\A").next();
		} finally {
			scanner.close();
		}
		assertEquals(TEST_CLASS_AFTER_SAVE_ACTIONS, actualContents);
	}

	@SuppressWarnings("restriction")
	private void enableJavaSaveActions() {
		InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).putBoolean(Constants.PERFORM_SAVE_ACTIONS_PREFERENCE, true);

		final Map<String, String> cleanupPreferences = new HashMap<String, String>(
				org.eclipse.jdt.internal.ui.JavaPlugin
						.getDefault()
						.getCleanUpRegistry()
						.getDefaultOptions(
								org.eclipse.jdt.internal.corext.fix.CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS)
						.getMap());

		cleanupPreferences.put(org.eclipse.jdt.internal.corext.fix.CleanUpConstants.FORMAT_SOURCE_CODE,
				CleanUpOptions.TRUE);
		cleanupPreferences.put(org.eclipse.jdt.internal.corext.fix.CleanUpConstants.ORGANIZE_IMPORTS,
				CleanUpOptions.TRUE);
		cleanupPreferences.put(org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS,
				CleanUpOptions.TRUE);
		org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil.saveSaveParticipantOptions(InstanceScope.INSTANCE,
				cleanupPreferences);
	}

	private IFolder createASourceFolder(final String name) throws CoreException {
		final IFolder aJavaSourceFolder = aJavaProject.getFolder(Path.fromPortableString(name));
		create(aJavaSourceFolder);
		return aJavaSourceFolder;
	}

	private IFolder createAPackage(final IFolder aJavaSourceFolder, final String name) throws CoreException {
		final IFolder aJavaPackage = aJavaSourceFolder.getFolder(Path.fromPortableString(name));
		create(aJavaPackage);
		return aJavaPackage;
	}

	private void create(final IFolder folder) throws CoreException {
		final IContainer parent = folder.getParent();
		if (parent.getType() == IResource.FOLDER && !parent.exists()) {
			create((IFolder) parent);
		}
		folder.create(true, true, NULL_PROGRESS_MONITOR);
	}

	private IFile createAJavaFile(final IFolder aJavaPackage, final String name) throws CoreException {
		final IFile aJavaFile = aJavaPackage.getFile(Path.fromPortableString(name));
		aJavaFile.create(new ByteArrayInputStream(TEST_CLASS_BEFORE_SAVE_ACTIONS.getBytes()), true,
				NULL_PROGRESS_MONITOR);
		return aJavaFile;
	}

	// based on http://www.stateofflow.com/journal/66/creating-java-projects-programmatically
	@SuppressWarnings("restriction")
	private IProject createAJavaProject(final String name) throws CoreException {
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(name);
		project.create(NULL_PROGRESS_MONITOR);
		project.open(NULL_PROGRESS_MONITOR);
		org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock.addJavaNature(project, new SubProgressMonitor(
				NULL_PROGRESS_MONITOR, 1));
		final IJavaProject javaProject = JavaCore.create(project);

		final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		for (final IClasspathEntry entry : javaProject.getRawClasspath()) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				((org.eclipse.jdt.internal.core.ClasspathEntry) entry).path = Path.fromPortableString(SOURCE_FOLDER);
			}
			entries.add(entry);
		}
		entries.add(JavaRuntime.getDefaultJREContainerEntry());
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), NULL_PROGRESS_MONITOR);

		return project;
	}
}
