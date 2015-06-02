package org.dawb.passerelle.common;

import org.dawb.common.services.IUserInputService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInputServiceHolder {
	
	private static final Logger logger = LoggerFactory.getLogger(UserInputServiceHolder.class);

	private IUserInputService   userInputService;

	private static UserInputServiceHolder current;
	
	public static UserInputServiceHolder getInstance() {
		if (current==null) current = new UserInputServiceHolder();
		return current;
	}
	
	public void start(ComponentContext context) {
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public IUserInputService getUserInputService() {
		return userInputService;
	}

	public void setUserInputService(IUserInputService userInputService) {
		this.userInputService = userInputService;
		logger.debug("Set "+userInputService.getClass().getSimpleName()+" in "+getClass().getName());
	}
}
