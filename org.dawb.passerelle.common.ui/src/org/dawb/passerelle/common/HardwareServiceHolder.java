package org.dawb.passerelle.common;

import org.dawb.common.services.IHardwareService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardwareServiceHolder {
	
	private static final Logger logger = LoggerFactory.getLogger(HardwareServiceHolder.class);

	private IHardwareService    hardwareService;

	private static HardwareServiceHolder current;
	
	public static HardwareServiceHolder getInstance() {
		if (current==null) current = new HardwareServiceHolder();
		return current;
	}
	
	public void start(ComponentContext context) {
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public IHardwareService getHardwareService() {
		return hardwareService;
	}

	public void setHardwareService(IHardwareService hardwareService) {
		this.hardwareService = hardwareService;
		logger.debug("Set "+hardwareService.getClass().getSimpleName()+" in "+getClass().getName());
	}
}
