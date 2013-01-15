package org.dawb.passerelle.views;

import java.util.Map;
import java.util.Queue;

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ActorDebugView extends ViewPart implements IRemoteWorkbenchPart{

	public static final String ID = "org.dawb.passerelle.views.actorDebugView";
	
	private ActorValuePage valuePage;
	private Queue<Object>  queue;
 
	@Override
	public void setUserObject(Object userObject) {
		final UserDebugBean  bean = (UserDebugBean)userObject;
		DataMessageComponent comp = (DataMessageComponent)bean.getDataMessageComponent();
		
		// TODO ensure ActorValuePage can deal with DataMessageComponent
	}

	@Override
	public void createPartControl(Composite parent) {
		valuePage = new ActorValuePage();
		valuePage.createControl(parent, false);
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
