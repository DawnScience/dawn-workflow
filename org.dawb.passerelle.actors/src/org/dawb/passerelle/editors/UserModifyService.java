/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import java.io.File;

import org.dawb.common.services.IUserInputService;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.workbench.jmx.RemoveWorkbenchPart;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

public class UserModifyService extends AbstractServiceFactory implements IUserInputService {

	@Override
	public Object create(Class serviceInterface, IServiceLocator parentLocator,
			IServiceLocator locator) {
		if (IUserInputService.class == serviceInterface) {
			return new UserModifyService();
		}
		return null;
	}

	@Override
	public RemoveWorkbenchPart openUserInputPart(final String partName, boolean isDialog) throws Exception {
		
		RemoveWorkbenchPart part;
		if (isDialog) {
			UserModifyDialog dialog = new UserModifyDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
			dialog.create();
			dialog.getShell().setText(partName);
			dialog.getShell().setSize(550, 480);
		    part = dialog;
		} else {
		    final IWorkbenchPage page = EclipseUtils.getActivePage();
		    final File tmp = File.createTempFile("review", "txt");
		    final IFileStore externalFile = EFS.getLocalFileSystem().fromLocalFile(tmp);
		    FileStoreEditorInput input = new FileStoreEditorInput(externalFile);
		    part = (RemoveWorkbenchPart)page.openEditor(input, "org.dawb.passerelle.editors.UserModifyEditor");
		}
		
		return part;
	}

}
