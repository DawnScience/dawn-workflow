package org.dawb.passerelle.ui.views;

import java.util.Map;
import java.util.Queue;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.passerelle.ui.Activator;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ActorDebugView extends ViewPart implements IRemoteWorkbenchPart{

	public static final String ID = "org.dawb.passerelle.views.actorDebugView";
	
	private ActorValuePage valuePage;
	private Queue<Object>  queue;
	private UserDebugBean  bean;
    private CLabel         partInfo;
	private Action play, stop;
 
	@Override
	public void setUserObject(Object userObject) {
		
		this.bean = (UserDebugBean)userObject;

		StringBuilder buf = new StringBuilder();
		if (bean.getPartName()!=null) {
			buf.append(bean.getPartName());
		} else {
			buf.append(" Debug '"+bean.getActorName()+"' ");
			if (bean.getPortName()!=null) buf.append("port '"+bean.getPortName()+"' ");
		}
		partInfo.setText(buf.toString());

        setEnabled(true);
		valuePage.setData(bean);

	}
	

	private void setEnabled(boolean enabled) {
		play.setEnabled(enabled);
		stop.setEnabled(enabled);
		valuePage.getActiveViewer().getControl().setEnabled(enabled);
		partInfo.setEnabled(enabled);
		if (!enabled) partInfo.setText("");
	}

	@Override
	public void createPartControl(Composite parent) {
		valuePage = new ActorValuePage();
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(main);
		
		this.partInfo = new CLabel(main, SWT.NONE);
		partInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		partInfo.setImage(Activator.getImageDescriptor("icons/information_purple.gif").createImage());	
		
		valuePage.createControl(main, false);
		valuePage.setTableView(true);
		valuePage.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		valuePage.getActiveViewer().getControl().setEnabled(false);
	
		createActions();
	}
	
	private void createActions() {
		this.play = new Action("Continue", Activator.getImageDescriptor("icons/run_workflow_purple.gif")) {
			public void run() {
				UserDebugBean ret = new UserDebugBean(bean.getScalar());
				ret.addScalar("debug_continue_time", String.valueOf(System.currentTimeMillis()));
				queue.add(ret);
				ActorDebugView.this.setEnabled(false);
			}
		};
		play.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(play);
		
		this.stop = new Action("Stop", Activator.getImageDescriptor("icons/stop_workflow_purple.gif")) {
			public void run() {
				queue.add(new UserDebugBean());
				ActorDebugView.this.setEnabled(false);
			}
		};
		stop.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(stop);
	}

	@Override
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	@Override
	public void setFocus() {
		if (valuePage!=null) valuePage.setFocus();
	}


	@Override
	public void setQueue(Queue<Object> valueQueue) {
		this.queue = valueQueue;
	}


	
/*$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$	
 	Methods not required in this context.
  $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$*/
	
	@Override
	public void setValues(Map<String, String> values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfiguration(String configurationXML) throws Exception {
		// TODO Auto-generated method stub
		
	}
/*$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ 	
	END  
  $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$*/

}
