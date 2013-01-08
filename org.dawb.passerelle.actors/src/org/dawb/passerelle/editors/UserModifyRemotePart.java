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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import org.dawb.common.ui.viewers.AppliableTableViewer;
import org.dawb.common.util.SubstituteUtils;
import org.dawb.common.util.text.NumberUtils;
import org.dawb.passerelle.actors.Activator;
import org.dawb.passerelle.actors.ui.config.FieldBean;
import org.dawb.passerelle.actors.ui.config.FieldContainer;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ResourceTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.gda.common.rcp.util.GridUtils;
import uk.ac.gda.richbeans.beans.BeanUI;
import uk.ac.gda.richbeans.beans.IFieldWidget;
import uk.ac.gda.richbeans.components.cell.FieldComponentCellEditor;
import uk.ac.gda.richbeans.components.file.FileBox;
import uk.ac.gda.richbeans.components.file.FileBox.ChoiceType;
import uk.ac.gda.richbeans.components.scalebox.NumberBox;
import uk.ac.gda.richbeans.components.wrappers.ComboWrapper;
import uk.ac.gda.richbeans.components.wrappers.SpinnerWrapper;
import uk.ac.gda.richbeans.components.wrappers.TextWrapper;

public class UserModifyRemotePart implements IRemoteWorkbenchPart {

	private static Logger logger = LoggerFactory.getLogger(UserModifyRemotePart.class);
	
	private String                     partName;
	private Closeable                  closeable;
	private AppliableTableViewer       tableViewer;
	private Queue<Object>              queue;
	private Map<String,String>         values;
	private Map<String,String>         originalValues;
	private FieldContainer             configuration;
	private Label                      customLabel;

    public UserModifyRemotePart() {
    	
    }
    
    @Override
	public void createRemotePart(final Object container, Closeable closeable) {
		
		final Composite contents = new Composite((Composite)container, SWT.NONE);
		this.closeable = closeable;
		contents.setLayout(new GridLayout(1, false));
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.customLabel  = new Label(contents, SWT.WRAP);
		customLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		final Image image = Activator.getDefault().getImageDescriptor("icons/information.gif").createImage();
		customLabel.setImage(image);
		GridUtils.setVisible(customLabel, false);
		
        this.tableViewer = new AppliableTableViewer(contents, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		
		createColumns(tableViewer);
		tableViewer.setUseHashlookup(true);
		tableViewer.setColumnProperties(new String[]{"Scalar Name", "Value"});
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
				
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {
				if (values==null || values.isEmpty()) return new Object[]{};
				final Collection<Map.Entry<String,String>> ret = values.entrySet();
				return	ret.toArray(new Map.Entry[ret.size()]); // Sorry horrible model, should use list of beans, it would be clearer.
			}
		});
		tableViewer.setInput(new Object());
		
		// Hack to change row height
		tableViewer.getTable().addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				// height cannot be per row so simply set
				event.height = 22;
			}
		});
		
		// We try to add a DND lister to allow files to be dragged and dropped here
		DropTarget dt = new DropTarget(tableViewer.getTable(), DND.DROP_MOVE| DND.DROP_DEFAULT| DND.DROP_COPY);
		dt.setTransfer(new Transfer[] { TextTransfer.getInstance (), FileTransfer.getInstance(), ResourceTransfer.getInstance()});
		dt.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
                final Object    data = event.data;
                final TableItem item = (TableItem)event.item;
                final Map.Entry<String, String> entry = (Map.Entry<String, String>)item.getData();
				final String valueName = ((Map.Entry<String, String>)entry).getKey();
				
				String path = null;
                if (data instanceof String[]) {
                	path = ((String[])data)[0];
                	
                } else if (data instanceof IResource[]) {
                	final IResource[] res = (IResource[])data;
                	path = res[0].getLocation().toOSString();
                	
                } else if (data instanceof File[]) {
                	path = ((File[])data)[0].getAbsolutePath();
                }
                
				if (path!=null && configuration!=null && configuration.getBean(valueName)!=null) {
					final FieldBean bean = configuration.getBean(valueName);
                    if (bean.getUiClass().equals(FileBox.class.getName())) {
                    	values.put(entry.getKey(), path);
                    	tableViewer.refresh();
                    }
				}

			}			
		});
	
	}
	
	public ColumnViewer getViewer() {
		return tableViewer;
	}
	private void createColumns(final TableViewer viewer) {
		
		ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE);
		
		TableViewerColumn name   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		name.getColumn().setText("");
		name.getColumn().setWidth(200);
		name.setLabelProvider(new ColumnLabelProvider() {
			public Color getForeground(Object element) {
				final String valueName = ((Map.Entry<String, String>)element).getKey();
				if (configuration!=null && configuration.getBean(valueName)!=null) {
					final FieldBean bean = configuration.getBean(valueName);
					if (bean.getUiLabel()!=null && !"".equals(bean.getUiLabel())) {
						return null;
					}
				}
				return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);	
			}
			public String getText(final Object element) {
				final String valueName = ((Map.Entry<String, String>)element).getKey();
				if (configuration!=null && configuration.getBean(valueName)!=null) {
					final FieldBean bean = configuration.getBean(valueName);
					if (bean.getUiLabel()!=null && !"".equals(bean.getUiLabel())) {
						return bean.getUiLabel();
					}
				}
				return valueName;
			}
			public String getToolTipText(Object element) {
				return "Variable name '"+((Map.Entry<String, String>)element).getKey()+"'";
			}
		});
		name.setEditingSupport(new EditingSupport(viewer) {			
			@Override
			protected void setValue(Object element, Object value) {
				
				final String val = values.remove(((Map.Entry<String, String>)element).getKey());
				if (value==null || "".equals(value)) return;
				values.put((String)value, val);
				viewer.refresh();
			}			
			@Override
			protected Object getValue(Object element) {
				return ((Map.Entry<String, String>)element).getKey();
			}		
			@Override
			protected CellEditor getCellEditor(Object element) {
				TextCellEditor ed = new TextCellEditor(viewer.getTable());
				ed.getControl().setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
				return ed;
			}			
			@Override
			protected boolean canEdit(Object element) {
				final String valueName = ((Map.Entry<String, String>)element).getKey();
				if (configuration!=null && configuration.containsBean(valueName)) return false;
				return true;
			}
		});
		
		TableViewerColumn value   = new TableViewerColumn(viewer, SWT.LEFT, 1);
		value.getColumn().setText("Value");
		value.getColumn().setWidth(600);
		value.setLabelProvider(new ColumnLabelProvider() {
			public String getText(final Object element) {
				final String varName = ((Map.Entry<String, String>)element).getKey();
				String val     = values.get(varName);
				
				if (configuration!=null && configuration.containsBean(varName)) {
					final FieldBean bean = configuration.getBean(varName);
					if (bean.getUnit()!=null && bean.getUiClass().endsWith(".StandardBox")) val = val+" "+bean.getUnit();
					if (bean.isPassword() && bean.getUiClass().endsWith(".TextWrapper")) val = getStars(val.length());
				}
				return val;
			}
		});
		value.setEditingSupport(new EditingSupport(viewer) {			
			@Override
			protected void setValue(Object element, Object value) {
				if (value!=null && !"".equals(value)) {
				    ((Map.Entry<String, String>)element).setValue(value.toString());
				} else {
					((Map.Entry<String, String>)element).setValue(null);
				}
				viewer.refresh();
			}			
			@Override
			protected Object getValue(Object element) {
				final String stringValue = ((Map.Entry<String, String>)element).getValue();
				return NumberUtils.getNumberIfParses(stringValue);
			}		
			@Override
			protected CellEditor getCellEditor(Object element) {
				final String valueName = ((Map.Entry<String, String>)element).getKey();
				if (configuration!=null && configuration.containsBean(valueName)) {
					return createFieldWidgetEditor(configuration.getBean(valueName));
				}
				return new TextCellEditor(viewer.getTable());
			}			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
	}
	
	protected String getStars(int length) {
		final StringBuilder buf = new StringBuilder();
		for (int i = 0; i < length; i++) buf.append("*");
		return buf.toString();
	}

	private CellEditor createFieldWidgetEditor(final FieldBean configBean) {
		
		final String uiClass = configBean.getUiClass();
		
		try {
			int style = SWT.NONE;
			if (configBean.getUiClass().equals(ComboWrapper.class.getName())) {
				style = SWT.READ_ONLY;
			} else if (configBean.getUiClass().equals(FileBox.class.getName())) {
				style = SWT.NO_TRIM;
			}
			
			if (configBean.isPassword()) style = style|SWT.PASSWORD;
			
			final FieldComponentCellEditor ed = new FieldComponentCellEditor(tableViewer.getTable(), uiClass, style);
			final IFieldWidget            wid = ed.getFieldWidget();
			
			// Could probably use some kind of reflection for this
			if (wid instanceof TextWrapper) {
				TextWrapper box = (TextWrapper)wid;
				if (configBean.getTextLimit()>0) box.setTextLimit(configBean.getTextLimit());
				
			} else if (wid instanceof ComboWrapper) {
				ComboWrapper box = (ComboWrapper)wid;
                final List<String> choices = configBean.getTextChoicesAsStrings();
                if(choices!=null && !choices.isEmpty()) {
                 	box.setItems(choices.toArray(new String[choices.size()]));
                }

			} else if (wid instanceof NumberBox) {
				NumberBox box = (NumberBox)wid;
				box.setDecimalPlaces(6);
				if (configBean.getLowerBound()!=null) box.setMinimum(configBean.getLowerBound().doubleValue());
				if (configBean.getUpperBound()!=null) box.setMaximum(configBean.getUpperBound().doubleValue());
				if (configBean.getUnit()!=null)       box.setUnit(configBean.getUnit());
				
			} else if (wid instanceof SpinnerWrapper) { 
				SpinnerWrapper box = (SpinnerWrapper)wid;
				if (configBean.getLowerBound()!=null) box.setMinimum(configBean.getLowerBound().intValue());
				if (configBean.getUpperBound()!=null) box.setMaximum(configBean.getUpperBound().intValue());
			
			} else if (wid instanceof FileBox) {
				FileBox box = (FileBox)wid;
				box.setChoiceType(ChoiceType.FULL_PATH);
				box.setFolder(configBean.isFolder());
				box.setFileTitle(configBean.isFolder()?"Choose Directory":"Choose File");
				if (configBean.getExtensions()!=null) box.setFilterExtensions(configBean.getExtensions().split(","));
			}
			return ed;

		} catch (ClassNotFoundException e) {
			logger.error("Cannot get cell editor for "+configBean, e);
			return new TextCellEditor(tableViewer.getTable());
		}	
	}

	
	protected void addScalar() {
		
		String name = "x";
		int num = 1;
		while(values.containsKey(name)) {
			if (!values.containsKey(name+num)) {
				name = name+num;
				break;
			}
			++num;
		}
		
		values.put(name, "<new scalar value>");
		tableViewer.refresh();
		
		// Edit selected Map.Entry<String, String>
		final Collection<Map.Entry<String, String>> contents = values.entrySet();
		Map.Entry<String, String> sel = null;
		for (Map.Entry<String, String> entry : contents) {
			if (entry.getKey().equals(name)) {
				sel = entry;
				break;
			}
		}
		
		tableViewer.editElement(sel, 1);
	}

	

	/**
	 * Create the actions.
	 */
	@Override
	public void initializeMenu(Object obars) {
		MenuManager man = new MenuManager();
		man.add(confirm);
		man.add(stop);
		man.add(new Separator(getClass().getName()+".sep1"));
		man.add(add);
		man.add(delete);
		final Menu menu = man.createContextMenu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);
	}
	
	@Override
	public void setConfiguration(String configurationXML) throws Exception {
		if (configurationXML==null) {
			configuration = null;
			return;
		}
		
		this.configuration =   (FieldContainer)BeanUI.getBean(configurationXML, FieldContainer.class.getClassLoader());
	}

	
	public boolean setFocus() {
		if (!tableViewer.getTable().isDisposed()) {
			return tableViewer.getTable().setFocus();
		}
		return false;
	}

	public void dispose() {
		
		if (tableViewer!=null&&!tableViewer.getTable().isDisposed())tableViewer.getTable().dispose();
		
		if (queue!=null) {
			queue.clear();
			if (queue!=null && originalValues!=null) {
				// Just in case something is waiting
				// An empty one cancels the message.
				if (queue.isEmpty()) queue.add(new HashMap<String,String>(0));
			}
		}
		this.queue          = null;
		this.values         = null;
		this.originalValues = null;
	}

	/**
	 * Queue must not be null and is cleared prior to using.
	 */
	public void setQueue(Queue<Object> queue) {
		this.queue = queue;
		queue.clear();
	}
	
	private boolean messageOnly = false;
	
	public void setValues(final Map<String, String> inputValues) {
		
		final Map<String,String> ovs;
		if (inputValues!=null) {
			ovs = new TreeMap<String, String>(inputValues);
		} else {
		    ovs = new TreeMap<String,String>();
		}
		if (configuration!=null) ovs.keySet().retainAll(configuration.getNames());
		this.originalValues=ovs;
		
		Map<String,String> sortedValues = new TreeMap<String,String>();
		sortedValues.putAll(originalValues);
		
		if (configuration!=null) for (String name : configuration.getNames()) {
			if (!sortedValues.containsKey(name)) {
				final FieldBean bean = (FieldBean)configuration.getBean(name);
				sortedValues.put(name, bean.getDefaultValue()!=null ? bean.getDefaultValue().toString() : "");
			}
		}
		
		this.values = new LinkedHashMap<String, String>(sortedValues.size());
		values.putAll(sortedValues);
		
		tableViewer.refresh();
		
		if (configuration!=null) if (configuration.getCustomLabel()!=null) {
			
			final String substituted = SubstituteUtils.substitute(configuration.getCustomLabel(), inputValues);
			customLabel.setText(substituted);
			GridUtils.setVisible(customLabel, true);
			customLabel.getParent().layout(new Control[]{customLabel});
			
			// If the values are empty, just show the message
			if (values!=null&& values.isEmpty()) {
				GridUtils.setVisible(tableViewer.getTable(), false);
				tableViewer.getTable().getParent().layout(new Control[]{tableViewer.getTable()});
				this.messageOnly=true;
			}
		}
	}

	@Override
	public boolean isMessageOnly() {
		return messageOnly;
	}
	
	// Actions used by class
	protected final Action confirm = new Action("Confirm values, close view and continue workflow.", Activator.getImageDescriptor("icons/application_form_confirm.png")) {
		public void run() {
			doConfirm();
			closeable.close();
		}
	};
	
	// Actions used by class
	protected final Action stop = new Action("Stop workflow downstream of this node.", Activator.getImageDescriptor("icons/stop_workflow.gif")) {
		public void run() {
			doStop();
			closeable.close();
		}
	};
	
	protected final Action add = new Action("Add a new scalar value", Activator.getImageDescriptor("icons/application_form_add.png")) {
		public void run() {
			addScalar();
		}
	};

	protected final Action delete = new Action("Delete selected scalar value", Activator.getImageDescriptor("icons/application_form_delete.png")) {
		public void run() {
			final ISelection sel = tableViewer.getSelection();
			if (sel!=null && sel instanceof StructuredSelection) {
				final Map.Entry<String, String> entry = (Map.Entry<String, String>)((StructuredSelection)sel).getFirstElement();
				if (entry!=null) {
					values.remove(entry.getKey());
					tableViewer.refresh();
				}
			}
		}
	};

	public String getPartName() {
		return partName;
	}

	protected void doConfirm() {
		tableViewer.applyEditorValue();
		tableViewer.cancelEditing();
		if (queue==null || values==null) {
			MessageDialog.open(MessageDialog.INFORMATION, Display.getCurrent().getActiveShell(),
					           "Cannot confirm", "The workflow is not waiting for you to confirm these values.\n\nThere is currently nothing to confirm.", SWT.NONE);
			return;
		}
		if (queue.isEmpty()) queue.add(values);
	}
	protected void doStop() {
		if (queue.isEmpty()) queue.add(new HashMap<String,String>(0));
		if (closeable!=null) closeable.close();
	}

	public void setPartName(String partName) {
		this.partName = partName;
	}

	@Override
	public void setUserObject(Object userObject) {
		throw new RuntimeException("Cannot call setUserObject(...) on "+getClass().getName());
	}
	
	@Override
    public void stop() {
		doStop();
	}

	@Override
    public void confirm() {
		doConfirm();
	}

	@Override
	public void setRemoteFocus() {
		setFocus();
	}

}
