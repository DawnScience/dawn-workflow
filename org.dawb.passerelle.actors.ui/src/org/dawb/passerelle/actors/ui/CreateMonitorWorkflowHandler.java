/*
 * Copyright 2012 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dawb.passerelle.actors.ui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.common.util.io.IFileSelector;
import org.dawb.passerelle.common.project.PasserelleNewProjectWizard;
import org.dawb.passerelle.common.project.PasserelleProjectUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.isencia.passerelle.workbench.model.editor.ui.editor.PasserelleModelMultiPageEditor;

public class CreateMonitorWorkflowHandler extends AbstractHandler {

	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
			final IFileSelector fileView   = (IFileSelector)EclipseUtils.getActivePage().getActivePart();
			Path     selected   = fileView.getSelectedPath();
			if (!Files.isDirectory(selected)) selected = selected.getParent();
			
			// Get any workflow project or create one called 'workflows'
			IProject project = EclipseUtils.getExistingProject("org.dawb.passerelle.common.PasserelleNature");
			if (project == null) {
				final PasserelleNewProjectWizard wizard = (PasserelleNewProjectWizard)EclipseUtils.openWizard("org.edna.passerelle.common.project.PasserelleWizard", true); // Quite old wizard id!
				project = wizard.getProjectCreated();
			}
			
			// Create workflow (moml) file
			final String name    = "Monitor_"+selected.getFileName().toString();
			final Path   toWrite = FileUtils.getUnique(selected, name, "moml");
			final IFile  file    = PasserelleProjectUtils.createFolderMonitorWorkflow(project, toWrite.getFileName().toString(), selected.toFile(), null);
			
			// Refresh project
			project.refreshLocal(IResource.DEPTH_ONE, null);
			
			// Open file in editor
			final PasserelleModelMultiPageEditor ed = (PasserelleModelMultiPageEditor)EclipseUtils.openEditor(file);
			
			// Go to edit tab
			ed.setPasserelleEditorActive();
			
	        return Boolean.TRUE;
        } catch (Exception ne) {
        	throw new ExecutionException("Cannot open file navigator part!", ne);
        }
	}

}
