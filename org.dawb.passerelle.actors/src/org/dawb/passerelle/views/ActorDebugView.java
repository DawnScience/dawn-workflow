package org.dawb.passerelle.views;

import java.util.Map;
import java.util.Queue;

import org.dawb.passerelle.actors.Activator;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ActorDebugView extends ViewPart implements IRemoteWorkbenchPart{

	public static final String ID = "org.dawb.passerelle.views.actorDebugView";
	
	private ActorValuePage valuePage;
	private Queue<Object>  queue;
	private UserDebugBean  bean;

	private Action play, stop;
 
	@Override
	public void setUserObject(Object userObject) {
		this.bean = (UserDebugBean)userObject;

		StringBuilder buf = new StringBuilder();
		if (bean.getPartName()!=null) {
			buf.append(bean.getPartName());
		} else {
			buf.append("Debug '"+bean.getActorName()+"' ");
			if (bean.getPortName()!=null) buf.append("port '"+bean.getPortName()+"' ");
		}
		setPartName(buf.toString());

		valuePage.setData(bean);
		play.setEnabled(true);
		stop.setEnabled(true);

	}

	@Override
	public void createPartControl(Composite parent) {
		valuePage = new ActorValuePage();
		valuePage.createControl(parent, false);
		valuePage.setTableView(true);
		
		createActions();
	}
	
	private void createActions() {
		this.play = new Action("Continue", Activator.getImageDescriptor("icons/run_workflow_purple.gif")) {
			public void run() {
				UserDebugBean ret = new UserDebugBean(bean.getScalar());
				ret.addScalar("debug_continue_time", String.valueOf(System.currentTimeMillis()));
				queue.add(ret);
				play.setEnabled(false);
				stop.setEnabled(false);
			}
		};
		play.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(play);
		
		this.stop = new Action("Stop", Activator.getImageDescriptor("icons/stop_workflow_purple.gif")) {
			public void run() {
				queue.add(new UserDebugBean());
				play.setEnabled(false);
				stop.setEnabled(false);
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
