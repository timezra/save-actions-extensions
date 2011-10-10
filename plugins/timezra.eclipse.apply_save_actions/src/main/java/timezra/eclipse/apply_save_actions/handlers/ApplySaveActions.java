package timezra.eclipse.apply_save_actions.handlers;

import static java.util.Arrays.asList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class ApplySaveActions extends AbstractHandler {

	private final IAdapterManager adapterManager;
	private final IWorkspace workspace;
	private final IWorkbench workbench;

	public ApplySaveActions() {
		this(Platform.getAdapterManager(), ResourcesPlugin.getWorkspace(), PlatformUI.getWorkbench());
	}

	ApplySaveActions(final IAdapterManager adapterManager, final IWorkspace workspace, final IWorkbench workbench) {
		this.adapterManager = adapterManager;
		this.workspace = workspace;
		this.workbench = workbench;
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);
		if (currentSelection instanceof IStructuredSelection) {
			final IStructuredSelection selections = (IStructuredSelection) currentSelection;
			try {
				applyTo(selections);
			} catch (final JavaModelException e) {
				throw new ExecutionException(Messages.APPLY_SAVE_ACTIONS_UNEXPECTED_ERROR, e);
			} catch (final InvocationTargetException e) {
				throw new ExecutionException(Messages.APPLY_SAVE_ACTIONS_UNEXPECTED_ERROR, e.getTargetException());
			}
		}
		return null;
	}

	private void applyTo(final IStructuredSelection selections) throws JavaModelException, InvocationTargetException {
		for (final Object o : selections.toList()) {
			final IJavaProject javaProject = getAdapter(o, IJavaProject.class);
			if (javaProject != null) {
				applyTo(javaProject.getPackageFragments());
				continue;
			}
			final IPackageFragmentRoot packageFragmentRoot = getAdapter(o, IPackageFragmentRoot.class);
			if (packageFragmentRoot != null) {
				applyTo(packageFragmentRoot);
				continue;
			}
			final IPackageFragment packageFragment = getAdapter(o, IPackageFragment.class);
			if (packageFragment != null) {
				applyTo(packageFragment);
				continue;
			}
			final ICompilationUnit compilationUnit = getAdapter(o, ICompilationUnit.class);
			if (compilationUnit != null) {
				applyTo(compilationUnit);
				continue;
			}
		}
	}

	private void applyTo(final IPackageFragmentRoot packageFragmentRoot) throws JavaModelException,
			InvocationTargetException {
		final IJavaElement[] children = packageFragmentRoot.getChildren();
		final IPackageFragment[] fragments = new IPackageFragment[children.length];
		System.arraycopy(children, 0, fragments, 0, children.length);
		applyTo(fragments);
	}

	private void applyTo(final IPackageFragment... packageFragments) throws JavaModelException,
			InvocationTargetException {
		final Collection<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
		for (final IPackageFragment f : packageFragments) {
			compilationUnits.addAll(asList(f.getCompilationUnits()));
		}
		applyTo(compilationUnits.toArray(new ICompilationUnit[compilationUnits.size()]));
	}

	private void applyTo(final ICompilationUnit... compilationUnits) throws InvocationTargetException {
		final IRunnableWithProgress delegate = new ApplySaveActionsOperation(compilationUnits);
		try {
			workbench.getProgressService().run(false, true, delegate);
		} catch (final InterruptedException e) {
			// cancellation is fine
		}
	}

	@SuppressWarnings("restriction")
	private IDocumentProvider createDocumentProvider() {
		return new org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider();
	}

	@SuppressWarnings("unchecked")
	private <T> T getAdapter(final Object o, final Class<T> c) {
		return (T) adapterManager.getAdapter(o, c);
	}

	private final class ApplySaveActionsOperation extends WorkspaceModifyOperation {
		private final ICompilationUnit[] compilationUnits;

		ApplySaveActionsOperation(final ICompilationUnit... compilationUnits) {
			this.compilationUnits = compilationUnits;
		}

		@Override
		public void execute(final IProgressMonitor pm) throws CoreException {
			pm.beginTask(Messages.APPLY_SAVE_ACTIONS_BEGIN_TASK, compilationUnits.length);
			try {
				for (final ICompilationUnit unit : compilationUnits) {
					applyTo(workspace.getRoot().getFile(unit.getPath()), pm);
				}
			} finally {
				pm.done();
			}
		}

		void applyTo(final IFile f, final IProgressMonitor pm) throws CoreException {
			report(f.getName(), pm);
			final IDocumentProvider provider = createDocumentProvider();
			final FileEditorInput editorInput = new FileEditorInput(f);
			try {
				provider.connect(editorInput);
				provider.aboutToChange(editorInput);
				provider.saveDocument(pm, editorInput, provider.getDocument(editorInput), true);
			} finally {
				provider.changed(editorInput);
				provider.disconnect(editorInput);
			}
		}

		void report(final String task, final IProgressMonitor pm) {
			if (pm.isCanceled()) {
				throw new OperationCanceledException();
			}
			pm.setTaskName(task);
			pm.worked(1);
		}
	}
}
